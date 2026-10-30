package com.ztna.service;

import com.ztna.model.SecurityLog;
import com.ztna.repository.SecurityLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SecurityLogService {

    private final SecurityLogRepository securityLogRepository;

    public SecurityLogService(SecurityLogRepository securityLogRepository) {
        this.securityLogRepository = securityLogRepository;
    }

    /**
     * Log an event with explicit severity.
     * severity: INFO | WARN | HIGH | CRITICAL
     */
    public SecurityLog logEvent(String username, String eventType,
                                String ipAddress, String description,
                                String severity) {
        SecurityLog log = new SecurityLog();
        log.setUsername(username);
        log.setEventType(eventType);
        log.setIpAddress(ipAddress);
        log.setDescription(description);
        log.setSeverity(severity != null ? severity : "INFO");
        log.setEventTime(LocalDateTime.now());
        return securityLogRepository.save(log);
    }

    /**
     * Log an event with explicit severity and sessionId.
     */
    public SecurityLog logEvent(String username, String eventType,
                                String ipAddress, String description,
                                String severity, String sessionId) {
        SecurityLog log = new SecurityLog();
        log.setUsername(username);
        log.setEventType(eventType);
        log.setIpAddress(ipAddress);
        log.setDescription(description);
        log.setSeverity(severity != null ? severity : "INFO");
        log.setSessionId(sessionId);
        log.setEventTime(LocalDateTime.now());
        return securityLogRepository.save(log);
    }

    /**
     * Convenience overload — defaults severity to INFO.
     * Keeps backward compatibility with existing callers.
     */
    public SecurityLog logEvent(String username, String eventType,
                                String ipAddress, String description) {
        return logEvent(username, eventType, ipAddress, description, "INFO");
    }

    public List<SecurityLog> getLogsByUsername(String username) {
        return securityLogRepository.findByUsernameOrderByEventTimeDesc(username);
    }

    public List<SecurityLog> getAllLogs() {
        return securityLogRepository.findAllByOrderByEventTimeDesc();
    }

    public List<SecurityLog> getLogsByEventType(String eventType) {
        return securityLogRepository.findByEventType(eventType);
    }

    public List<SecurityLog> getLogsBySeverity(String severity) {
        return securityLogRepository.findBySeverity(severity);
    }
}
