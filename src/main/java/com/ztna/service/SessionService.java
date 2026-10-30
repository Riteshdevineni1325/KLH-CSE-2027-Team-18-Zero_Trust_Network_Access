package com.ztna.service;

import com.ztna.model.Session;
import com.ztna.model.User;
import com.ztna.repository.SessionRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {

    private static final long SESSION_TIMEOUT_MINUTES = 30;

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public Session createSession(User user, String ipAddress,
                                  String deviceInfo, String deviceFingerprint) {
        return createSession(user, ipAddress, deviceInfo, deviceFingerprint, "UNKNOWN");
    }

    public Session createSession(User user, String ipAddress,
                                  String deviceInfo, String deviceFingerprint,
                                  String networkClassification) {
        Session session = new Session();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUser(user);
        session.setIpAddress(ipAddress);
        session.setDeviceInfo(deviceInfo);
        session.setDeviceFingerprint(deviceFingerprint);
        session.setNetworkClassification(networkClassification != null ? networkClassification : "UNKNOWN");
        session.setLoginTime(LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(SESSION_TIMEOUT_MINUTES));
        session.setActive(true);
        return sessionRepository.save(session);
    }

    // ── Validate ──────────────────────────────────────────────────────────────

    /**
     * Returns the active session if valid, empty otherwise.
     * Heartbeats lastActivity on success. Auto-terminates on idle timeout.
     */
    public Optional<Session> validateSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return Optional.empty();

        Optional<Session> opt = sessionRepository.findBySessionIdAndActiveTrue(sessionId);
        if (opt.isEmpty()) return Optional.empty();

        Session session = opt.get();
        LocalDateTime now = LocalDateTime.now();

        if (session.getLastActivity() != null &&
                Duration.between(session.getLastActivity(), now).toMinutes() > SESSION_TIMEOUT_MINUTES) {
            terminateSession(sessionId, "TIMEOUT");
            return Optional.empty();
        }

        session.setLastActivity(now);
        session.setExpiresAt(now.plusMinutes(SESSION_TIMEOUT_MINUTES));
        return Optional.of(sessionRepository.save(session));
    }

    // ── Terminate ─────────────────────────────────────────────────────────────

    public void terminateSession(String sessionId, String reason) {
        sessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setActive(false);
            session.setStatus("TERMINATED");
            session.setTerminationReason(reason != null ? reason : "UNKNOWN");
            sessionRepository.save(session);
        });
    }

    /** Backward-compatible overload — defaults reason to LOGOUT. */
    public void terminateSession(String sessionId) {
        terminateSession(sessionId, "LOGOUT");
    }

    public void terminateUserSessions(String username, String reason) {
        List<Session> active = sessionRepository.findByUser_UsernameAndActiveTrue(username);
        for (Session s : active) {
            s.setActive(false);
            s.setStatus("TERMINATED");
            s.setTerminationReason(reason != null ? reason : "ADMIN");
        }
        sessionRepository.saveAll(active);
    }

    /** Backward-compatible overload — defaults reason to ADMIN. */
    public void terminateUserSessions(String username) {
        terminateUserSessions(username, "ADMIN");
    }

    public void terminateDeviceSessions(String deviceFingerprint, String reason) {
        List<Session> active = sessionRepository.findByActiveTrue();
        for (Session s : active) {
            if (deviceFingerprint.equalsIgnoreCase(s.getDeviceFingerprint())) {
                s.setActive(false);
                s.setStatus("TERMINATED");
                s.setTerminationReason(reason != null ? reason : "DEVICE_BLOCKED");
                sessionRepository.save(s);
            }
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<Session> getActiveSessions() {
        return sessionRepository.findByActiveTrue();
    }

    public List<Session> getActiveSessionsByUsername(String username) {
        return sessionRepository.findByUser_UsernameAndActiveTrue(username);
    }

    public Optional<Session> findBySessionId(String sessionId) {
        return sessionRepository.findBySessionId(sessionId);
    }
}
