package com.ztna.service;

import com.ztna.model.Session;
import com.ztna.repository.SessionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SessionActivityService {

    private final SessionRepository sessionRepository;

    public SessionActivityService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public String evaluateActivity(
            String username,
            int failedAttempts) {

        // Rule 1: High failed login attempts trigger suspicious rating
        if (failedAttempts >= 3) {
            return "SUSPICIOUS";
        }

        // Rule 2: Multi-origin active sessions from distinct IPs
        if (username != null && !username.isBlank()) {
            List<Session> activeSessions = sessionRepository.findByUser_UsernameAndActiveTrue(username);
            Set<String> distinctIps = activeSessions.stream()
                    .map(Session::getIpAddress)
                    .filter(ip -> ip != null && !ip.isBlank() && !"UNKNOWN".equalsIgnoreCase(ip))
                    .collect(Collectors.toSet());

            if (distinctIps.size() > 1) {
                return "SUSPICIOUS";
            }
        }

        return "NORMAL";
    }
}