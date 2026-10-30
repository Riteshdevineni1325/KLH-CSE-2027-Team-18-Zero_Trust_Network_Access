package com.ztna.controller;

import com.ztna.dto.AccessRequest;
import com.ztna.dto.PolicyEvaluationResult;
import com.ztna.service.ContextCollectorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint for testing and performing explicit access requests to the PDP.
 */
@RestController
@RequestMapping("/api/access")
public class AccessController {

    private final ContextCollectorService contextCollectorService;

    public AccessController(ContextCollectorService contextCollectorService) {
        this.contextCollectorService = contextCollectorService;
    }

    @PostMapping("/request")
    public ResponseEntity<?> requestAccess(@RequestBody AccessRequest request,
                                           HttpServletRequest httpRequest) {
        try {
            String resourceName = request.getResourceName() != null ? request.getResourceName() : "GENERAL_PROTECTED";
            PolicyEvaluationResult result = contextCollectorService.collectAndEvaluate(
                    request.getUsername(), resourceName, httpRequest);

            int status = result.isAllowed() ? HttpStatus.OK.value() : HttpStatus.FORBIDDEN.value();

            return ResponseEntity.status(status).body(Map.of(
                    "username", request.getUsername(),
                    "resourceName", resourceName,
                    "decision", result.getDecision(),
                    "policyName", result.getPolicyName(),
                    "reason", result.getReason(),
                    "networkClassification", result.getNetworkClassification(),
                    "deviceStatus", result.getDeviceStatus(),
                    "role", result.getUserRole()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Access evaluation failed: " + e.getMessage()));
        }
    }
}
