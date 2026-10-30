package com.ztna.controller;

import com.ztna.dto.LoginRequest;
import com.ztna.dto.LoginResponse;
import com.ztna.dto.RegisterRequest;
import com.ztna.model.AdminPromotionRequest;
import com.ztna.model.Session;
import com.ztna.model.User;
import com.ztna.repository.AdminPromotionRequestRepository;
import com.ztna.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication Controller for CAR-ZTNA.
 * Implements Feature 1 (Authentication) and Feature 2 (Account Lockout
 * Integration).
 * Strict adherence to Design Rules: Zero numerical scores; binary ALLOW/DENY;
 * explicit context.
 */
@RestController
@RequestMapping({ "/api/auth", "/api/users" })
public class AuthController {

        private final AuthService authService;
        private final ContextCollectorService contextCollectorService;
        private final SecurityLogService securityLogService;
        private final SessionService sessionService;
        private final LoginAttemptService loginAttemptService;
        private final NetworkService networkService;
        private final DeviceService deviceService;
        private final AdminPromotionRequestRepository adminPromotionRequestRepository;

        public AuthController(AuthService authService,
                        ContextCollectorService contextCollectorService,
                        SecurityLogService securityLogService,
                        SessionService sessionService,
                        LoginAttemptService loginAttemptService,
                        NetworkService networkService,
                        DeviceService deviceService,
                        AdminPromotionRequestRepository adminPromotionRequestRepository) {
                this.authService = authService;
                this.contextCollectorService = contextCollectorService;
                this.securityLogService = securityLogService;
                this.sessionService = sessionService;
                this.loginAttemptService = loginAttemptService;
                this.networkService = networkService;
                this.deviceService = deviceService;
                this.adminPromotionRequestRepository = adminPromotionRequestRepository;
        }

        // ── Registration ─────────────────────────────────────────────────────────

        @PostMapping("/register")
        public ResponseEntity<?> register(@RequestBody RegisterRequest req, HttpServletRequest httpRequest) {
                try {
                        User user = authService.registerUser(req.getUsername(), req.getPassword(), req.getRole());
                        user.setPassword(null);

                        String ip = contextCollectorService.extractClientIp(httpRequest);

                        // Record the registration IP as OFFICE for this user
                        networkService.recordRegistrationIp(user.getUsername(), ip);

                        securityLogService.logEvent(user.getUsername(), "REGISTER_SUCCESS", ip,
                                        "User registered with role: " + user.getRole(), "INFO");

                        return ResponseEntity.status(HttpStatus.CREATED).body(user);
                } catch (RuntimeException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
                }
        }

        // ── Login ─────────────────────────────────────────────────────────────────

        @PostMapping("/login")
        public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest httpRequest) {
                String clientIp = contextCollectorService.extractClientIp(httpRequest);

                try {
                        // 1. Authenticate credentials (verifies password and account status)
                        User user = authService.authenticate(req.getUsername(), req.getPassword());

                        // 2. Classify Network and Resolve Device
                        String networkClass = networkService.classifyIpForUser(user.getUsername(), clientIp);

                        // Record the IP in the per-user network address table (auto-labels as
                        // OFFICE/HOME)
                        networkService.observeIp(user.getUsername(), clientIp);

                        String userAgent = httpRequest.getHeader("User-Agent");
                        String customDevice = httpRequest.getHeader("X-Device-Name");
                        String rawDevice = (customDevice != null && !customDevice.isBlank())
                                        ? customDevice
                                        : (userAgent != null && !userAgent.isBlank() ? userAgent
                                                        : "Unknown Workstation");

                        String os = deviceService.extractOs(userAgent);
                        String browser = deviceService.extractBrowser(userAgent);
                        String fingerprint = deviceService.computeFingerprint(user.getUsername(), rawDevice, os,
                                        browser);
                        String deviceStatus = deviceService.resolveDeviceStatus(user.getUsername(), rawDevice, os,
                                        browser,
                                        clientIp);

                        // 3. Network or Device Hard Block Check
                        if ("BLOCKED".equalsIgnoreCase(networkClass)) {
                                securityLogService.logEvent(user.getUsername(), "LOGIN_BLOCKED_NETWORK", clientIp,
                                                "Login denied: Originating IP " + clientIp
                                                                + " is classified as BLOCKED.",
                                                "HIGH");
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(LoginResponse
                                                                .denied("Access denied: Your network IP is blocked."));
                        }

                        if ("BLOCKED".equalsIgnoreCase(deviceStatus)) {
                                securityLogService.logEvent(user.getUsername(), "LOGIN_BLOCKED_DEVICE", clientIp,
                                                "Login denied: Device '" + rawDevice + "' is BLOCKED by administrator.",
                                                "HIGH");
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(LoginResponse.denied(
                                                                "Access denied: This device is blocked by administrator."));
                        }

                        // 4. Issue Active Session (includes network classification)
                        Session session = sessionService.createSession(user, clientIp, rawDevice, fingerprint,
                                        networkClass);

                        securityLogService.logEvent(user.getUsername(), "LOGIN_SUCCESS", clientIp,
                                        "User logged in successfully [Network: " + networkClass + ", Device: "
                                                        + deviceStatus + "]",
                                        "INFO", session.getSessionId());

                        user.setPassword(null);

                        LoginResponse response = new LoginResponse(
                                        user,
                                        "ALLOW",
                                        session.getSessionId(),
                                        networkClass,
                                        deviceStatus,
                                        "Authentication successful.");

                        return ResponseEntity.ok(response);

                } catch (RuntimeException e) {
                        return handleLoginFailure(req.getUsername(), clientIp, e);
                }
        }

        private ResponseEntity<?> handleLoginFailure(String username, String ip, RuntimeException e) {
                String msg = e.getMessage();

                if ("ACCOUNT_LOCKED".equals(msg)) {
                        securityLogService.logEvent(username, "LOGIN_BLOCKED", ip,
                                        "Login attempt on locked account (threshold: 5 failed attempts)", "HIGH");
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(LoginResponse.denied(
                                                        "Account is locked due to 5 consecutive failed login attempts. Administrator recovery required."));
                }

                if ("ACCOUNT_SUSPENDED".equals(msg)) {
                        securityLogService.logEvent(username, "LOGIN_BLOCKED", ip,
                                        "Login attempt on suspended account", "HIGH");
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(LoginResponse.denied("Account is suspended by administrator."));
                }

                // Check if this is a real user before recording or surfacing attempt counts.
                // Non-existent usernames must not accumulate attempt records and must not
                // reveal count information (which would signal that the user exists).
                boolean userExists = authService.usernameExists(username);

                if (!userExists) {
                        // Log silently but do not increment attempts or reveal count
                        securityLogService.logEvent(username, "LOGIN_FAILED", ip,
                                        "Failed login attempt (unknown username)", "WARN");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(LoginResponse.denied("Invalid username or password."));
                }

                // Existing account — read the attempt count (already recorded inside
                // AuthService.authenticate)
                securityLogService.logEvent(username, "LOGIN_FAILED", ip, "Failed login attempt (invalid credentials)",
                                "WARN");
                int currentFails = loginAttemptService.getFailedAttempts(username);

                if (loginAttemptService.isLocked(username)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(LoginResponse.denied(
                                                        "Account has been locked after 5 consecutive failed attempts."));
                }

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(LoginResponse
                                                .denied("Invalid username or password. Consecutive failed attempts: "
                                                                + currentFails + "/5"));
        }

        // ── Logout ────────────────────────────────────────────────────────────────

        @PostMapping("/logout")
        public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                        @RequestParam(value = "sessionId", required = false) String paramSessionId,
                        HttpServletRequest httpRequest) {
                String sessionId = null;
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        sessionId = authHeader.substring(7).trim();
                } else if (paramSessionId != null && !paramSessionId.isBlank()) {
                        sessionId = paramSessionId.trim();
                }

                if (sessionId != null) {
                        sessionService.terminateSession(sessionId, "LOGOUT");
                        securityLogService.logEvent("SESSION", "LOGOUT",
                                        contextCollectorService.extractClientIp(httpRequest),
                                        "Session terminated: " + sessionId, "INFO", sessionId);
                        return ResponseEntity
                                        .ok(Map.of("status", "success", "message", "User logged out successfully."));
                }

                return ResponseEntity.badRequest().body(Map.of("error", "No active session token provided."));
        }

        // ── Admin Promotion Request (regular user submits) ────────────────────────

        @PostMapping("/promotion-request")
        public ResponseEntity<?> requestAdminPromotion(@RequestBody Map<String, String> body,
                        HttpServletRequest request) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of("error", "Authentication required."));
                }
                String sessionId = authHeader.substring(7).trim();
                var sessionOpt = sessionService.validateSession(sessionId);
                if (sessionOpt.isEmpty() || sessionOpt.get().getUser() == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of("error", "Invalid or expired session."));
                }
                String username = sessionOpt.get().getUser().getUsername();
                String reason = body.getOrDefault("reason", "").trim();
                if (reason.isEmpty()) {
                        return ResponseEntity.badRequest().body(Map.of("error", "A reason is required."));
                }
                AdminPromotionRequest req = new AdminPromotionRequest(username, reason);
                adminPromotionRequestRepository.save(req);
                securityLogService.logEvent(username, "ADMIN_PROMOTION_REQUESTED",
                                contextCollectorService.extractClientIp(request),
                                "User submitted admin promotion request: " + reason, "INFO");
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(Map.of("status", "success",
                                                "message",
                                                "Promotion request submitted. An administrator will review it."));
        }
}
