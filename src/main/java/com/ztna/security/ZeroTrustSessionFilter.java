package com.ztna.security;

import com.ztna.dto.PolicyEvaluationResult;
import com.ztna.model.Session;
import com.ztna.model.User;
import com.ztna.service.ContextCollectorService;
import com.ztna.service.PolicyDecisionService;
import com.ztna.service.SecurityLogService;
import com.ztna.service.SessionService;
import com.ztna.service.SuperKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Policy Enforcement Point (PEP).
 * Implements Feature 9: Enforces continuous authorization on every protected
 * backend request.
 *
 * Strict Design Rules:
 * - NO numerical risk/trust scores.
 * - Deterministic binary ALLOW or DENY.
 * - Default-Deny: unpermitted paths or unmatched policies are rejected.
 * - Explicit deny policies override allow policies.
 * - All permissions enforced strictly on the backend.
 */
@Component
public class ZeroTrustSessionFilter extends OncePerRequestFilter {

    private final SessionService sessionService;
    private final ContextCollectorService contextCollectorService;
    private final PolicyDecisionService policyDecisionService;
    private final SecurityLogService securityLogService;
    private final SuperKeyService superKeyService;

    public ZeroTrustSessionFilter(SessionService sessionService,
            ContextCollectorService contextCollectorService,
            PolicyDecisionService policyDecisionService,
            SecurityLogService securityLogService,
            SuperKeyService superKeyService) {
        this.sessionService = sessionService;
        this.contextCollectorService = contextCollectorService;
        this.policyDecisionService = policyDecisionService;
        this.securityLogService = securityLogService;
        this.superKeyService = superKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        boolean isProtected = path.startsWith("/api/protected");
        boolean isAdmin = path.startsWith("/api/admin");
        boolean isAudit = path.startsWith("/api/audit");

        // Public routes bypass PEP
        if (!isProtected && !isAdmin && !isAudit) {
            chain.doFilter(request, response);
            return;
        }

        // ── 1. Session Token Validation ─────────────────────────────────────────
        String sessionId = contextCollectorService.extractSessionId(request);
        if (sessionId == null || sessionId.isBlank()) {
            sendUnauthorized(response, "Authentication required. Provide a valid Bearer session token.");
            return;
        }

        Optional<Session> sessionOpt = sessionService.validateSession(sessionId);
        if (sessionOpt.isEmpty()) {
            sendUnauthorized(response, "Session is invalid, expired, or terminated.");
            return;
        }

        Session session = sessionOpt.get();
        User user = session.getUser();
        if (user == null) {
            sendUnauthorized(response, "Session has no associated user identity.");
            return;
        }

        // ── 2. Account State Check (LOCKED state terminates access immediately) ──
        if (user.isLocked() || "LOCKED".equalsIgnoreCase(user.getStatus())) {
            sessionService.terminateSession(sessionId, "ACCOUNT_LOCKED");
            securityLogService.logEvent(
                    user.getUsername(),
                    "PEP_DENY",
                    contextCollectorService.extractClientIp(request),
                    "Request rejected: User account is currently locked.",
                    "HIGH",
                    sessionId);
            sendForbidden(response, "Access Denied: Account is locked. Administrator recovery required.");
            return;
        }

        String username = user.getUsername();
        String role = user.getRole() != null ? user.getRole().toUpperCase() : "USER";

        // ── 3. Administrative / Audit Role Guard ────────────────────────────────
        if (isAdmin && !"ADMIN".equals(role)) {
            securityLogService.logEvent(username, "PEP_DENY", contextCollectorService.extractClientIp(request),
                    "Access denied to admin endpoint: Role is " + role, "WARN", sessionId);
            sendForbidden(response, "Access Denied: ADMIN role required.");
            return;
        }

        // Context report: any authenticated user may access their own report.
        // /api/audit/context-report/{username} and alias
        // /api/audit/risk-report/{username}
        // — USER can only see their own.
        boolean isOwnContextReport = path.startsWith("/api/audit/context-report/")
                || path.startsWith("/api/audit/risk-report/");
        if (isOwnContextReport) {
            String[] parts = path.split("/");
            String targetUser = parts.length > 0 ? parts[parts.length - 1] : "";
            if (!"ADMIN".equals(role) && !username.equals(targetUser)) {
                sendForbidden(response, "Access Denied: You can only view your own context report.");
                return;
            }
            // Allow through — no further audit guard needed for this path
        } else if (isAudit && !"ADMIN".equals(role)) {
            securityLogService.logEvent(username, "PEP_DENY", contextCollectorService.extractClientIp(request),
                    "Access denied to audit endpoint: Role is " + role, "WARN", sessionId);
            sendForbidden(response, "Access Denied: ADMIN role required for audit endpoints.");
            return;
        }

        // ── 4. Protected Resource Context Evaluation (PDP) ──────────────────────
        if (isProtected) {
            String resourceName = resolveResourceNameFromPath(path);

            PolicyEvaluationResult result = contextCollectorService.collectAndEvaluate(username, resourceName, request);

            if (!result.isAllowed()) {
                securityLogService.logEvent(
                        username,
                        "PEP_DENY",
                        contextCollectorService.extractClientIp(request),
                        "Access DENIED to resource '" + resourceName + "'. Reason: " + result.getReason(),
                        "HIGH",
                        sessionId);

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write(String.format(
                        "{\"error\":\"Forbidden\",\"decision\":\"DENY\",\"resource\":\"%s\",\"reason\":\"%s\"}",
                        resourceName, result.getReason()));
                return;
            }

            // Access Granted by PDP
            securityLogService.logEvent(
                    username,
                    "PEP_ALLOW",
                    contextCollectorService.extractClientIp(request),
                    "Access GRANTED to resource '" + resourceName + "'. Policy: " + result.getPolicyName(),
                    "INFO",
                    sessionId);

            request.setAttribute("ztnaSession", session);
            request.setAttribute("ztnaEvaluation", result);
        }

        // ── 5. Set Spring Security Authentication Context ──────────────────────
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username, null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);
    }

    private String resolveResourceNameFromPath(String path) {
        // e.g. /api/protected/financial-data -> FINANCIAL_DATA
        String clean = path.replace("/api/protected/", "").replace("/", "");
        if (clean.isBlank())
            return "GENERAL_PROTECTED";
        return clean.toUpperCase().replace("-", "_");
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"" + message + "\"}");
    }
}
