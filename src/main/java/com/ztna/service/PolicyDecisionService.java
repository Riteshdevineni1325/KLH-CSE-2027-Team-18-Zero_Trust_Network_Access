package com.ztna.service;

import com.ztna.dto.PolicyEvaluationResult;
import com.ztna.model.AccessPolicy;
import com.ztna.model.ContextSnapshot;
import com.ztna.repository.AccessPolicyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Policy Decision Point (PDP).
 * Implements Feature 7: RBAC and ABAC-style rules evaluated using explicit contextual conditions.
 *
 * Strict Design Rules:
 * - NO numerical risk scores or trust scores.
 * - NO password-based risk scoring.
 * - Binary decisions ONLY: ALLOW or DENY (NO global RESTRICT).
 * - Default-Deny: If no applicable policy permits access, deny by default.
 * - Explicit Deny Precedence: Explicit deny policies take precedence over conflicting allow policies.
 */
@Service
public class PolicyDecisionService {

    private final AccessPolicyRepository accessPolicyRepository;

    public PolicyDecisionService(AccessPolicyRepository accessPolicyRepository) {
        this.accessPolicyRepository = accessPolicyRepository;
    }

    /**
     * Evaluates access request against contextual parameters.
     *
     * @param username              Identity of the requester
     * @param userRole              Role (USER, ADMIN)
     * @param networkClassification OFFICE, HOME, UNKNOWN, BLOCKED
     * @param deviceStatus          TRUSTED, UNREGISTERED, BLOCKED
     * @param isAccountLocked       Account lockout flag
     * @param resourceName          Target resource identifier
     * @return PolicyEvaluationResult with binary ALLOW or DENY and explicit reason
     */
    public PolicyEvaluationResult evaluateAccess(String username,
                                                  String userRole,
                                                  String networkClassification,
                                                  String deviceStatus,
                                                  boolean isAccountLocked,
                                                  String resourceName) {

        // 1. Account Lockout Check: Locked accounts are immediately rejected
        if (isAccountLocked) {
            return PolicyEvaluationResult.deny("ACCOUNT_LOCKOUT_POLICY", resourceName,
                    "Account is currently locked due to security policy violations.",
                    networkClassification, deviceStatus, userRole);
        }

        // 2. Network Perimeter Block Check: Blocked networks cannot access any resource
        if ("BLOCKED".equalsIgnoreCase(networkClassification)) {
            return PolicyEvaluationResult.deny("NETWORK_BLOCK_POLICY", resourceName,
                    "Access request originated from a strictly blocked network IP/CIDR range.",
                    networkClassification, deviceStatus, userRole);
        }

        // 3. Device Block Check: Compromised/blocked hardware is rejected
        if ("BLOCKED".equalsIgnoreCase(deviceStatus)) {
            return PolicyEvaluationResult.deny("DEVICE_BLOCK_POLICY", resourceName,
                    "Access request originated from a blocked/revoked hardware device.",
                    networkClassification, deviceStatus, userRole);
        }

        // 4. Query policies defined for the target resource (ordered by priority)
        List<AccessPolicy> policies = accessPolicyRepository
                .findByResourceNameAndEnabledTrueOrderByPriorityAsc(resourceName);

        // Design Rule: Default-Deny when no active policy is configured for the resource
        if (policies.isEmpty()) {
            return PolicyEvaluationResult.deny("DEFAULT_DENY", resourceName,
                    "Default-Deny: No active access policy is defined for resource '" + resourceName + "'.",
                    networkClassification, deviceStatus, userRole);
        }

        // 5. Explicit Deny Precedence Check:
        // Any matching policy with effect == "DENY" immediately overrides any ALLOW policy
        for (AccessPolicy policy : policies) {
            if ("DENY".equalsIgnoreCase(policy.getEffect())) {
                if (matchesPolicyConditions(policy, userRole, networkClassification, deviceStatus)) {
                    return PolicyEvaluationResult.deny(policy.getPolicyName(), resourceName,
                            "Explicit Deny Precedence: Policy '" + policy.getPolicyName() +
                                    "' explicitly denied access for context [Role: " + userRole +
                                    ", Network: " + networkClassification + ", Device: " + deviceStatus + "].",
                            networkClassification, deviceStatus, userRole);
                }
            }
        }

        // 6. Evaluate Allow Policies:
        for (AccessPolicy policy : policies) {
            if ("ALLOW".equalsIgnoreCase(policy.getEffect())) {
                if (matchesPolicyConditions(policy, userRole, networkClassification, deviceStatus)) {
                    return PolicyEvaluationResult.allow(policy.getPolicyName(), resourceName,
                            "Access granted under policy '" + policy.getPolicyName() + "'.",
                            networkClassification, deviceStatus, userRole);
                }
            }
        }

        // 7. Fallback: Default-Deny if no ALLOW policy matched the specific context
        return PolicyEvaluationResult.deny("DEFAULT_DENY", resourceName,
                "Default-Deny: Ambient context does not satisfy any configured ALLOW policy for resource '"
                        + resourceName + "'.",
                networkClassification, deviceStatus, userRole);
    }

    /**
     * Helper to evaluate RBAC and ABAC conditions of an AccessPolicy against ambient context.
     */
    private boolean matchesPolicyConditions(AccessPolicy policy,
                                            String userRole,
                                            String networkClassification,
                                            String deviceStatus) {
        // RBAC Check
        if (!"ANY".equalsIgnoreCase(policy.getRequiredRole())) {
            if (!policy.getRequiredRole().equalsIgnoreCase(userRole)) {
                return false;
            }
        }

        // ABAC Check: Allowed Network
        if (!"ANY".equalsIgnoreCase(policy.getAllowedNetwork())) {
            if (!policy.getAllowedNetwork().equalsIgnoreCase(networkClassification)) {
                return false;
            }
        }

        // ABAC Check: Required Device Status
        if (!"ANY".equalsIgnoreCase(policy.getRequiredDeviceStatus())) {
            if (!policy.getRequiredDeviceStatus().equalsIgnoreCase(deviceStatus)) {
                return false;
            }
        }

        return true;
    }
}
