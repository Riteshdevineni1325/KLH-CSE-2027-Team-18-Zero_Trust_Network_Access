package com.ztna.controller;

import com.ztna.model.AccessPolicy;
import com.ztna.repository.AccessPolicyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final AccessPolicyRepository accessPolicyRepository;

    public PolicyController(AccessPolicyRepository accessPolicyRepository) {
        this.accessPolicyRepository = accessPolicyRepository;
    }

    @GetMapping
    public ResponseEntity<List<AccessPolicy>> getAllPolicies() {
        return ResponseEntity.ok(accessPolicyRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPolicyById(@PathVariable Long id) {
        return accessPolicyRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Policy not found with id: " + id));
    }

    @PostMapping
    public ResponseEntity<AccessPolicy> createOrUpdatePolicy(@RequestBody AccessPolicy policy) {
        AccessPolicy saved = accessPolicyRepository.save(policy);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> togglePolicy(@PathVariable Long id) {
        return accessPolicyRepository.findById(id)
                .<ResponseEntity<?>>map(policy -> {
                    policy.setEnabled(!policy.isEnabled());
                    return ResponseEntity.ok(accessPolicyRepository.save(policy));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Policy not found with id: " + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePolicy(@PathVariable Long id) {
        if (!accessPolicyRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Policy not found with id: " + id);
        }
        accessPolicyRepository.deleteById(id);
        return ResponseEntity.ok("Policy deleted successfully");
    }
}
