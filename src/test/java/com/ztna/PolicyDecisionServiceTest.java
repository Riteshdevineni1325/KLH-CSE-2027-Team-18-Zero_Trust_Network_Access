package com.ztna;

import com.ztna.dto.PolicyEvaluationResult;
import com.ztna.model.AccessPolicy;
import com.ztna.repository.AccessPolicyRepository;
import com.ztna.service.PolicyDecisionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class PolicyDecisionServiceTest {

    private AccessPolicyRepository accessPolicyRepository;
    private PolicyDecisionService policyDecisionService;

    @BeforeEach
    void setUp() {
        accessPolicyRepository = Mockito.mock(AccessPolicyRepository.class);
        policyDecisionService = new PolicyDecisionService(accessPolicyRepository);
    }

    @Test
    @DisplayName("Design Rule: Default-Deny when no applicable policy permits access")
    void testDefaultDenyWhenNoPolicies() {
        when(accessPolicyRepository.findByResourceNameAndEnabledTrueOrderByPriorityAsc("SECRET_DOCS"))
                .thenReturn(List.of());

        PolicyEvaluationResult result = policyDecisionService.evaluateAccess(
                "alice", "USER", "OFFICE", "TRUSTED", false, "SECRET_DOCS");

        assertFalse(result.isAllowed());
        assertEquals("DENY", result.getDecision());
        assertTrue(result.getReason().contains("Default-Deny"));
    }

    @Test
    @DisplayName("Design Rule: Explicit DENY takes precedence over conflicting ALLOW policies")
    void testExplicitDenyPrecedence() {
        AccessPolicy allowPolicy = new AccessPolicy(
                "Allow Office Users", "FINANCIAL_DATA", "USER", "OFFICE", "ANY", "ALLOW", 10, "Allow rule");
        AccessPolicy denyPolicy = new AccessPolicy(
                "Deny Untrusted Hardware", "FINANCIAL_DATA", "ANY", "ANY", "UNREGISTERED", "DENY", 20, "Deny rule");

        // Even though allowPolicy has lower priority number (10 vs 20), explicit DENY overrides
        when(accessPolicyRepository.findByResourceNameAndEnabledTrueOrderByPriorityAsc("FINANCIAL_DATA"))
                .thenReturn(List.of(allowPolicy, denyPolicy));

        PolicyEvaluationResult result = policyDecisionService.evaluateAccess(
                "eve", "USER", "OFFICE", "UNREGISTERED", false, "FINANCIAL_DATA");

        assertFalse(result.isAllowed());
        assertEquals("DENY", result.getDecision());
        assertTrue(result.getReason().contains("Explicit Deny Precedence"));
    }

    @Test
    @DisplayName("Context Evaluation: Successful RBAC + ABAC matches grant ALLOW")
    void testSuccessfulAllowEvaluation() {
        AccessPolicy policy = new AccessPolicy(
                "Financial Office Trusted", "FINANCIAL_DATA", "USER", "OFFICE", "TRUSTED", "ALLOW", 10, "Standard rule");

        when(accessPolicyRepository.findByResourceNameAndEnabledTrueOrderByPriorityAsc("FINANCIAL_DATA"))
                .thenReturn(List.of(policy));

        PolicyEvaluationResult result = policyDecisionService.evaluateAccess(
                "alice", "USER", "OFFICE", "TRUSTED", false, "FINANCIAL_DATA");

        assertTrue(result.isAllowed());
        assertEquals("ALLOW", result.getDecision());
        assertEquals("Financial Office Trusted", result.getPolicyName());
    }

    @Test
    @DisplayName("Security Guard: Locked accounts are immediately rejected with DENY")
    void testLockedAccountImmediateDeny() {
        PolicyEvaluationResult result = policyDecisionService.evaluateAccess(
                "eve", "USER", "OFFICE", "TRUSTED", true, "FINANCIAL_DATA");

        assertFalse(result.isAllowed());
        assertEquals("DENY", result.getDecision());
        assertTrue(result.getReason().contains("locked"));
    }

    @Test
    @DisplayName("Security Guard: Requests from BLOCKED networks are immediately rejected with DENY")
    void testBlockedNetworkImmediateDeny() {
        PolicyEvaluationResult result = policyDecisionService.evaluateAccess(
                "alice", "USER", "BLOCKED", "TRUSTED", false, "FINANCIAL_DATA");

        assertFalse(result.isAllowed());
        assertEquals("DENY", result.getDecision());
        assertTrue(result.getReason().contains("blocked network"));
    }
}
