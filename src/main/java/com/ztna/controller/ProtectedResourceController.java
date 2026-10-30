package com.ztna.controller;

import com.ztna.dto.PolicyEvaluationResult;
import com.ztna.model.Session;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for Least-Privilege Protected Resources.
 * Implements Feature 8: Protects resources individually and grants access
 * strictly based on PDP deterministic ALLOW decision.
 */
@RestController
@RequestMapping("/api/protected")
public class ProtectedResourceController {

    @GetMapping("/resource")
    public ResponseEntity<?> getGenericProtectedResource(HttpServletRequest request) {
        Session session = (Session) request.getAttribute("ztnaSession");
        PolicyEvaluationResult eval = (PolicyEvaluationResult) request.getAttribute("ztnaEvaluation");

        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Active session required."));
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "decision", "ALLOW",
                "resource", "GENERAL_PROTECTED",
                "user", session.getUser().getUsername(),
                "role", session.getUser().getRole(),
                "policy", eval != null ? eval.getPolicyName() : "DEFAULT_ALLOW",
                "message", "Zero Trust Policy Check Passed. Access Granted."
        ));
    }

    @GetMapping("/financial-data")
    public ResponseEntity<?> getFinancialData(HttpServletRequest request) {
        return buildResourceResponse("FINANCIAL_DATA", "Q3 Revenue: $4.2M, Operating Expense: $2.1M, Net Margin: 50%", request);
    }

    @GetMapping("/internal-documents")
    public ResponseEntity<?> getInternalDocs(HttpServletRequest request) {
        return buildResourceResponse("INTERNAL_DOCUMENTS", "Engineering Architecture Specs and ZTNA Guidelines v2.0", request);
    }

    @GetMapping("/employee-directory")
    public ResponseEntity<?> getEmployeeDirectory(HttpServletRequest request) {
        return buildResourceResponse("EMPLOYEE_DIRECTORY", "Internal Corporate Directory: 240 active staff members", request);
    }

    @GetMapping("/hr-records")
    public ResponseEntity<?> getHrRecords(HttpServletRequest request) {
        return buildResourceResponse("HR_RECORDS", "Confidential Personnel Records & Payroll Management", request);
    }

    @GetMapping("/source-code-repo")
    public ResponseEntity<?> getSourceCode(HttpServletRequest request) {
        return buildResourceResponse("SOURCE_CODE_REPO", "Git Repository: git@internal.corp.ztna:core/car-ztna-engine.git", request);
    }

    @GetMapping("/admin-console")
    public ResponseEntity<?> getAdminConsole(HttpServletRequest request) {
        return buildResourceResponse("ADMIN_CONSOLE", "Master Administrative Configuration Console", request);
    }

    @GetMapping("/network-topology")
    public ResponseEntity<?> getNetworkTopology(HttpServletRequest request) {
        return buildResourceResponse("NETWORK_TOPOLOGY", "Subnet Maps: Internal 192.168.1.0/24, DMZ 10.0.0.0/16", request);
    }

    private ResponseEntity<?> buildResourceResponse(String resourceName, String payload, HttpServletRequest request) {
        Session session = (Session) request.getAttribute("ztnaSession");
        PolicyEvaluationResult eval = (PolicyEvaluationResult) request.getAttribute("ztnaEvaluation");

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "resource", resourceName,
                "data", payload,
                "user", session != null ? session.getUser().getUsername() : "UNKNOWN",
                "policyMatched", eval != null ? eval.getPolicyName() : "VERIFIED_PEP"
        ));
    }
}
