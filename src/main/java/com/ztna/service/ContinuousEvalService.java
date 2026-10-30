package com.ztna.service;

import com.ztna.model.Device;
import com.ztna.model.Session;
import com.ztna.model.User;
import com.ztna.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Continuous Session Evaluation & Revocation Service.
 * Implements Feature 10: Continuously monitors active sessions and automatically
 * revokes sessions when accounts are LOCKED, devices are BLOCKED, or networks are BLOCKED.
 */
@Service
public class ContinuousEvalService {

    private static final Logger log = LoggerFactory.getLogger(ContinuousEvalService.class);

    private final SessionService sessionService;
    private final NetworkService networkService;
    private final DeviceRepository deviceRepository;
    private final SecurityLogService securityLogService;

    public ContinuousEvalService(SessionService sessionService,
                                 NetworkService networkService,
                                 DeviceRepository deviceRepository,
                                 SecurityLogService securityLogService) {
        this.sessionService = sessionService;
        this.networkService = networkService;
        this.deviceRepository = deviceRepository;
        this.securityLogService = securityLogService;
    }

    /**
     * Periodic assessment runs every 60 seconds to detect any security revocation events.
     */
    @Scheduled(fixedRate = 60 * 1000)
    @Transactional
    public void validateActiveSessions() {
        List<Session> activeSessions = sessionService.getActiveSessions();
        if (activeSessions.isEmpty()) return;

        log.debug("Continuous session validation running for {} active sessions", activeSessions.size());

        for (Session session : activeSessions) {
            try {
                reassessSession(session);
            } catch (Exception e) {
                log.error("Error evaluating session '{}': {}", session.getSessionId(), e.getMessage());
            }
        }
    }

    public void reassessSession(Session session) {
        User user = session.getUser();
        String username = user != null ? user.getUsername() : "UNKNOWN";

        // 1. Account Lockout Check
        if (user != null && (user.isLocked() || "LOCKED".equalsIgnoreCase(user.getStatus()))) {
            terminateAndLog(session, "ACCOUNT_LOCKED", "Account for user '" + username + "' is locked.");
            return;
        }

        // 2. Network Block Check
        String netClass = networkService.classifyIpForUser(username, session.getIpAddress());
        if ("BLOCKED".equalsIgnoreCase(netClass)) {
            terminateAndLog(session, "NETWORK_BLOCKED", "Session originating IP " + session.getIpAddress() + " was blocked.");
            return;
        }

        // 3. Device Block Check
        if (session.getDeviceFingerprint() != null && user != null) {
            Optional<Device> dev = deviceRepository.findByUser_UsernameAndDeviceFingerprint(username, session.getDeviceFingerprint());
            if (dev.isPresent() && "BLOCKED".equalsIgnoreCase(dev.get().getStatus())) {
                terminateAndLog(session, "DEVICE_BLOCKED", "Device '" + dev.get().getDeviceName() + "' was marked BLOCKED by admin.");
                return;
            }
        }
    }

    private void terminateAndLog(Session session, String reason, String details) {
        String username = session.getUser() != null ? session.getUser().getUsername() : "UNKNOWN";
        sessionService.terminateSession(session.getSessionId(), reason);

        securityLogService.logEvent(
                username,
                "SESSION_TERMINATED",
                session.getIpAddress(),
                details + " Session revoked immediately: " + session.getSessionId(),
                "HIGH",
                session.getSessionId()
        );

        log.warn("Terminated session '{}' for user '{}': {}", session.getSessionId(), username, reason);
    }
}
