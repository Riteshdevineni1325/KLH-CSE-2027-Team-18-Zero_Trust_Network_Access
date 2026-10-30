package com.ztna.service;

import org.springframework.stereotype.Service;

/**
 * Policy Enforcement Service.
 * Deterministic binary mapping: ALLOW -> GRANTED, DENY -> DENIED.
 * Strictly avoids numerical risk scores and RESTRICT status.
 */
@Service
public class PolicyEnforcementService {

    public record EnforcementResult(String decision, String message) {}

    public EnforcementResult enforce(String decision, String resourceName, String username) {
        if ("ALLOW".equalsIgnoreCase(decision)) {
            return new EnforcementResult("ALLOW", "Access granted to resource '" + resourceName + "' for user: " + username);
        } else {
            return new EnforcementResult("DENY", "Access denied to resource '" + resourceName + "' for user: " + username);
        }
    }
}
