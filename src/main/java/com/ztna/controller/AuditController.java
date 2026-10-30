package com.ztna.controller;

import com.ztna.dto.AdminStats;
import com.ztna.dto.ContextReportResponse;
import com.ztna.model.SecurityLog;
import com.ztna.model.Session;
import com.ztna.repository.SecurityLogRepository;
import com.ztna.service.ReportService;
import com.ztna.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Audit & Telemetry Controller.
 * Implements Feature 11: PostgreSQL audit log viewing and context inspection.
 * Gated by ZeroTrustSessionFilter.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final SecurityLogRepository securityLogRepository;
    private final SessionService sessionService;
    private final ReportService reportService;

    public AuditController(SecurityLogRepository securityLogRepository,
                           SessionService sessionService,
                           ReportService reportService) {
        this.securityLogRepository = securityLogRepository;
        this.sessionService = sessionService;
        this.reportService = reportService;
    }

    // ── 1. Security Logs ──────────────────────────────────────────────────────

    @GetMapping("/logs")
    public ResponseEntity<List<SecurityLog>> getLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String severity) {

        if (username != null && !username.isBlank()) {
            return ResponseEntity.ok(securityLogRepository.findByUsernameOrderByEventTimeDesc(username));
        }
        if (eventType != null && !eventType.isBlank()) {
            return ResponseEntity.ok(securityLogRepository.findByEventType(eventType));
        }
        if (severity != null && !severity.isBlank()) {
            return ResponseEntity.ok(securityLogRepository.findBySeverity(severity.toUpperCase()));
        }
        return ResponseEntity.ok(securityLogRepository.findAllByOrderByEventTimeDesc());
    }

    @GetMapping("/logs/user/{username}")
    public ResponseEntity<List<SecurityLog>> getLogsByUser(@PathVariable String username) {
        return ResponseEntity.ok(securityLogRepository.findByUsernameOrderByEventTimeDesc(username));
    }

    // ── 2. Active Sessions ────────────────────────────────────────────────────

    @GetMapping("/sessions/active")
    public ResponseEntity<List<Session>> getActiveSessions() {
        return ResponseEntity.ok(sessionService.getActiveSessions());
    }

    @GetMapping("/sessions/user/{username}")
    public ResponseEntity<List<Session>> getActiveSessionsByUser(@PathVariable String username) {
        return ResponseEntity.ok(sessionService.getActiveSessionsByUsername(username));
    }

    // ── 3. Context & Zero Trust Posture Report ─────────────────────────────────

    @GetMapping({"/context-report/{username}", "/risk-report/{username}"})
    public ResponseEntity<?> getContextReport(@PathVariable String username) {
        ContextReportResponse report = reportService.buildContextReport(username);
        if (report == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No user profile or context found for: " + username));
        }
        return ResponseEntity.ok(report);
    }

    // ── 4. Dashboard Stats ────────────────────────────────────────────────────

    @GetMapping("/dashboard/stats")
    public ResponseEntity<AdminStats> getDashboardStats() {
        return ResponseEntity.ok(reportService.buildDashboardStats());
    }
}
