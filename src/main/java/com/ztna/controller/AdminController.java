package com.ztna.controller;

import com.ztna.dto.*;
import com.ztna.model.*;
import com.ztna.repository.*;
import com.ztna.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Administrator API Controller.
 * Implements Features 3 (Super Key Recovery), 4 (Dual IP/CIDR Network Rules),
 * 5 (Device Management), 7 (Policy Engine Config), 10 (Session Revocation),
 * and 12 (Admin Dashboard Statistics).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceService deviceService;
    private final NetworkService networkService;
    private final AccessPolicyRepository accessPolicyRepository;
    private final SessionService sessionService;
    private final LoginAttemptService loginAttemptService;
    private final SecurityLogRepository securityLogRepository;
    private final SecurityLogService securityLogService;
    private final SuperKeyService superKeyService;
    private final ContextCollectorService contextCollectorService;
    private final AdminPromotionRequestRepository adminPromotionRequestRepository;
    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository,
                           DeviceRepository deviceRepository,
                           DeviceService deviceService,
                           NetworkService networkService,
                           AccessPolicyRepository accessPolicyRepository,
                           SessionService sessionService,
                           LoginAttemptService loginAttemptService,
                           SecurityLogRepository securityLogRepository,
                           SecurityLogService securityLogService,
                           SuperKeyService superKeyService,
                           ContextCollectorService contextCollectorService,
                           AdminPromotionRequestRepository adminPromotionRequestRepository) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.deviceService = deviceService;
        this.networkService = networkService;
        this.accessPolicyRepository = accessPolicyRepository;
        this.sessionService = sessionService;
        this.loginAttemptService = loginAttemptService;
        this.securityLogRepository = securityLogRepository;
        this.securityLogService = securityLogService;
        this.superKeyService = superKeyService;
        this.contextCollectorService = contextCollectorService;
        this.adminPromotionRequestRepository = adminPromotionRequestRepository;
        this.passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    // ── 1. User Management ───────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{username}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable String username,
                                            @RequestParam String role,
                                            @RequestBody(required = false) AdminApprovalRequest approvalReq,
                                            HttpServletRequest request) {
        String adminUsername = resolveAdminUsername(request);

        // If promoting to ADMIN, enforce dual-admin verification (second admin password required)
        if ("ADMIN".equalsIgnoreCase(role)) {
            if (approvalReq == null || approvalReq.getSecondAdminUsername() == null || approvalReq.getSecondAdminPassword() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Dual Authorization Required: Promoting a user to ADMIN requires another administrator's username and password."));
            }

            String secondAdminUser = approvalReq.getSecondAdminUsername().trim();
            String secondAdminPass = approvalReq.getSecondAdminPassword().trim();

            if (secondAdminUser.equalsIgnoreCase(adminUsername)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Dual Authorization Violation: Second approving administrator must be a different administrator than yourself."));
            }

            User secondAdmin = userRepository.findByUsername(secondAdminUser).orElse(null);
            if (secondAdmin == null || !"ADMIN".equalsIgnoreCase(secondAdmin.getRole()) || secondAdmin.isLocked()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Second administrator verification failed: Invalid administrator account or account is locked."));
            }

            if (!passwordEncoder.matches(secondAdminPass, secondAdmin.getPassword())) {
                securityLogService.logEvent(adminUsername, "ADMIN_DUAL_AUTH_FAILED",
                        contextCollectorService.extractClientIp(request),
                        "Dual-authorization failed for promotion of '" + username + "': invalid password for second admin '" + secondAdminUser + "'", "HIGH");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Second administrator verification failed: Incorrect password."));
            }
        }

        return userRepository.findByUsername(username).map(user -> {
            user.setRole(role.toUpperCase());
            userRepository.save(user);

            securityLogService.logEvent(adminUsername, "ADMIN_USER_ROLE_CHANGED",
                    contextCollectorService.extractClientIp(request),
                    "Role for user '" + username + "' updated to: " + role + (role.equalsIgnoreCase("ADMIN") ? " (Dual Admin Verified)" : ""), "INFO");

            return ResponseEntity.ok(Map.of("status", "success", "message", "Role updated successfully."));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found.")));
    }

    // ── Admin Promotion Requests ──────────────────────────────────────────────

    @GetMapping("/promotion-requests")
    public ResponseEntity<List<AdminPromotionRequest>> getPromotionRequests() {
        return ResponseEntity.ok(adminPromotionRequestRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/promotion-requests/{id}/approve")
    public ResponseEntity<?> approvePromotionRequest(@PathVariable Long id,
                                                     @RequestBody AdminApprovalRequest approvalReq,
                                                     HttpServletRequest request) {
        String adminUsername = resolveAdminUsername(request);

        if (approvalReq == null || approvalReq.getSecondAdminUsername() == null || approvalReq.getSecondAdminPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Dual Authorization Required: Approving an admin promotion requires another administrator's username and password."));
        }

        String secondAdminUser = approvalReq.getSecondAdminUsername().trim();
        String secondAdminPass = approvalReq.getSecondAdminPassword().trim();

        if (secondAdminUser.equalsIgnoreCase(adminUsername)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Dual Authorization Violation: Second approving administrator must be different from yourself."));
        }

        User secondAdmin = userRepository.findByUsername(secondAdminUser).orElse(null);
        if (secondAdmin == null || !"ADMIN".equalsIgnoreCase(secondAdmin.getRole()) || secondAdmin.isLocked()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Second administrator verification failed: Invalid admin account or account is locked."));
        }

        if (!passwordEncoder.matches(secondAdminPass, secondAdmin.getPassword())) {
            securityLogService.logEvent(adminUsername, "ADMIN_DUAL_AUTH_FAILED",
                    contextCollectorService.extractClientIp(request),
                    "Dual-authorization failed for promotion request #" + id + ": invalid password for second admin '" + secondAdminUser + "'", "HIGH");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Second administrator verification failed: Incorrect password."));
        }

        AdminPromotionRequest promotionReq = adminPromotionRequestRepository.findById(id).orElse(null);
        if (promotionReq == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Promotion request not found."));
        }

        if (!"PENDING".equalsIgnoreCase(promotionReq.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Request is already " + promotionReq.getStatus()));
        }

        User user = userRepository.findByUsername(promotionReq.getRequesterUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Requester user no longer exists."));
        }

        user.setRole("ADMIN");
        userRepository.save(user);

        promotionReq.setStatus("APPROVED");
        promotionReq.setApprovedByAdmin(adminUsername);
        promotionReq.setSecondAdminUsername(secondAdminUser);
        promotionReq.setResolvedAt(java.time.LocalDateTime.now());
        adminPromotionRequestRepository.save(promotionReq);

        securityLogService.logEvent(adminUsername, "ADMIN_PROMOTION_APPROVED",
                contextCollectorService.extractClientIp(request),
                "User '" + user.getUsername() + "' promoted to ADMIN with dual verification by " + adminUsername + " and " + secondAdminUser, "INFO");

        return ResponseEntity.ok(Map.of("status", "success", "message", "User '" + user.getUsername() + "' has been approved and promoted to ADMIN."));
    }

    @PostMapping("/promotion-requests/{id}/reject")
    public ResponseEntity<?> rejectPromotionRequest(@PathVariable Long id, HttpServletRequest request) {
        String adminUsername = resolveAdminUsername(request);

        AdminPromotionRequest promotionReq = adminPromotionRequestRepository.findById(id).orElse(null);
        if (promotionReq == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Promotion request not found."));
        }

        if (!"PENDING".equalsIgnoreCase(promotionReq.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Request is already " + promotionReq.getStatus()));
        }

        promotionReq.setStatus("REJECTED");
        promotionReq.setApprovedByAdmin(adminUsername);
        promotionReq.setResolvedAt(java.time.LocalDateTime.now());
        adminPromotionRequestRepository.save(promotionReq);

        securityLogService.logEvent(adminUsername, "ADMIN_PROMOTION_REJECTED",
                contextCollectorService.extractClientIp(request),
                "Admin promotion request for user '" + promotionReq.getRequesterUsername() + "' rejected by " + adminUsername, "INFO");

        return ResponseEntity.ok(Map.of("status", "success", "message", "Promotion request rejected."));
    }

    @org.springframework.transaction.annotation.Transactional
    @DeleteMapping("/users/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username, HttpServletRequest request) {
        String adminUsername = resolveAdminUsername(request);
        if (username.equalsIgnoreCase(adminUsername)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Cannot delete your own active administrator account."));
        }
        return userRepository.findByUsername(username).map(user -> {
            sessionService.terminateUserSessions(username, "USER_DELETED");
            loginAttemptService.deleteAttemptsByUsername(username);
            userRepository.delete(user);

            securityLogService.logEvent(adminUsername, "ADMIN_USER_DELETED",
                    contextCollectorService.extractClientIp(request),
                    "User '" + username + "' deleted by admin.", "WARN");

            return ResponseEntity.ok(Map.of("status", "success", "message", "User deleted successfully."));
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found.")));
    }

    // ── 2. Super Key Recovery (Feature 3) ────────────────────────────────────

    /**
     * Generates a 4-digit, session-bound, short-lived Super Key.
     * No target username is required at generation time — the key is scoped to the
     * admin session and the target is supplied only when redeeming (unlocking).
     * Only the SHA-256 hash of the plaintext key is stored in PostgreSQL.
     */
    @PostMapping("/super-key/generate")
    public ResponseEntity<?> generateSuperKey(HttpServletRequest request) {
        String sessionId     = contextCollectorService.extractSessionId(request);
        String adminUsername = resolveAdminUsername(request);

        // Generate with a placeholder target; actual target supplied at redeem time
        String superKey = superKeyService.generateSuperKey(sessionId, adminUsername, "__PENDING__");
        return ResponseEntity.ok(Map.of(
                "status",  "success",
                "superKey", superKey,
                "message", "Super Key generated. Valid for 10 minutes, single-use. Do NOT share."
        ));
    }

    /**
     * Redeems a Super Key to unlock a locked user account.
     */
    @PostMapping("/super-key/unlock")
    public ResponseEntity<?> redeemSuperKey(@RequestBody SuperKeyUnlockRequest req, HttpServletRequest request) {
        String sessionId = contextCollectorService.extractSessionId(request);
        String adminUsername = resolveAdminUsername(request);

        boolean unlocked = superKeyService.redeemSuperKeyAndUnlock(
                sessionId,
                adminUsername,
                req.getTargetUsername(),
                req.getSuperKey()
        );

        if (unlocked) {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Account for '" + req.getTargetUsername() + "' successfully unlocked using Super Key."
            ));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "error",
                    "message", "Invalid, expired, or previously redeemed Super Key."
            ));
        }
    }

    // ── 3. Dual IP / Network Rules Management (Feature 4) ────────────────────

    @GetMapping("/networks")
    public ResponseEntity<List<NetworkRule>> getAllNetworkRules() {
        return ResponseEntity.ok(networkService.getAllRules());
    }

    @PostMapping("/networks")
    public ResponseEntity<?> createOrUpdateNetworkRule(@RequestBody NetworkRuleRequest req, HttpServletRequest request) {
        String adminUsername = resolveAdminUsername(request);
        NetworkRule rule = networkService.saveRule(req.getIpOrCidr(), req.getClassification(), req.getDescription());

        securityLogService.logEvent(adminUsername, "ADMIN_NETWORK_RULE_SAVED",
                contextCollectorService.extractClientIp(request),
                "Network rule configured: " + req.getIpOrCidr() + " -> " + req.getClassification(), "INFO");

        return ResponseEntity.ok(rule);
    }

    @DeleteMapping("/networks/{id}")
    public ResponseEntity<?> deleteNetworkRule(@PathVariable Long id, HttpServletRequest request) {
        networkService.deleteRule(id);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Network rule deleted."));
    }

    // ── 4. Device Management (Feature 5) ─────────────────────────────────────

    @GetMapping("/devices")
    public ResponseEntity<List<Device>> getAllDevices() {
        return ResponseEntity.ok(deviceRepository.findAll());
    }

    @PostMapping("/devices/{id}/status")
    public ResponseEntity<?> updateDeviceStatus(@PathVariable Long id,
                                                @RequestParam String status,
                                                HttpServletRequest request) {
        Device device = deviceService.setDeviceStatus(id, status);

        // If blocked, terminate all active sessions using this device
        if ("BLOCKED".equalsIgnoreCase(status)) {
            sessionService.terminateDeviceSessions(device.getDeviceFingerprint(), "DEVICE_BLOCKED");
        }

        return ResponseEntity.ok(device);
    }

    @DeleteMapping("/devices/{id}")
    public ResponseEntity<?> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Device deleted."));
    }

    // ── 5. Access Policy Engine Management (Feature 7) ───────────────────────

    @GetMapping("/policies")
    public ResponseEntity<List<AccessPolicy>> getAllPolicies() {
        return ResponseEntity.ok(accessPolicyRepository.findAll());
    }

    @PostMapping("/policies")
    public ResponseEntity<?> createOrUpdatePolicy(@RequestBody AccessPolicyRequest req, HttpServletRequest request) {
        String pName = req.getPolicyName() != null ? req.getPolicyName().trim() : "";
        String rName = req.getResourceName() != null ? req.getResourceName().trim() : "";
        String role = req.getRequiredRole() != null ? req.getRequiredRole().trim() : "ANY";
        String net = req.getAllowedNetwork() != null ? req.getAllowedNetwork().trim() : "ANY";
        String dev = req.getRequiredDeviceStatus() != null ? req.getRequiredDeviceStatus().trim() : "ANY";
        String eff = req.getEffect() != null ? req.getEffect().trim() : "ALLOW";

        if (pName.isEmpty() || rName.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Policy name and resource name are required."));
        }

        // Check for duplicates
        List<AccessPolicy> allExisting = accessPolicyRepository.findAll();
        for (AccessPolicy existing : allExisting) {
            // Ignore if updating same policy ID
            if (req.getId() != null && existing.getId().equals(req.getId())) {
                continue;
            }
            if (existing.getPolicyName().trim().equalsIgnoreCase(pName)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "status", "error",
                        "message", "A policy with the name '" + pName + "' already exists."
                ));
            }
            if (existing.getResourceName().trim().equalsIgnoreCase(rName)
                    && existing.getRequiredRole().trim().equalsIgnoreCase(role)
                    && existing.getAllowedNetwork().trim().equalsIgnoreCase(net)
                    && existing.getRequiredDeviceStatus().trim().equalsIgnoreCase(dev)
                    && existing.getEffect().trim().equalsIgnoreCase(eff)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "status", "error",
                        "message", "A policy matching these exact resource, role, network, device status, and effect rules already exists ('" + existing.getPolicyName() + "')."
                ));
            }
        }

        AccessPolicy policy;
        if (req.getId() != null && accessPolicyRepository.existsById(req.getId())) {
            policy = accessPolicyRepository.findById(req.getId()).get();
            policy.setPolicyName(pName);
            policy.setResourceName(rName);
            policy.setRequiredRole(role);
            policy.setAllowedNetwork(net);
            policy.setRequiredDeviceStatus(dev);
            policy.setEffect(eff);
            policy.setPriority(req.getPriority());
            policy.setDescription(req.getDescription());
            policy.setUpdatedAt(java.time.LocalDateTime.now());
        } else {
            policy = new AccessPolicy(
                    pName,
                    rName,
                    role,
                    net,
                    dev,
                    eff,
                    req.getPriority(),
                    req.getDescription()
            );
        }
        AccessPolicy saved = accessPolicyRepository.save(policy);

        securityLogService.logEvent(resolveAdminUsername(request), "ADMIN_POLICY_SAVED",
                contextCollectorService.extractClientIp(request),
                "Access Policy saved: " + saved.getPolicyName() + " (" + saved.getEffect() + ")", "INFO");

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/policies/{id}")
    public ResponseEntity<?> deletePolicy(@PathVariable Long id) {
        accessPolicyRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Policy deleted."));
    }

    // ── 6. Session Management (Feature 10) ───────────────────────────────────

    @GetMapping("/sessions")
    public ResponseEntity<List<Session>> getActiveSessions() {
        return ResponseEntity.ok(sessionService.getActiveSessions());
    }

    @PostMapping("/sessions/terminate/{sessionId}")
    public ResponseEntity<?> terminateSession(@PathVariable String sessionId) {
        sessionService.terminateSession(sessionId, "ADMIN_TERMINATED");
        return ResponseEntity.ok(Map.of("status", "success", "message", "Session terminated."));
    }

    @PostMapping("/sessions/terminate-user/{username}")
    public ResponseEntity<?> terminateUserSessions(@PathVariable String username) {
        sessionService.terminateUserSessions(username, "ADMIN_TERMINATED");
        return ResponseEntity.ok(Map.of("status", "success", "message", "User sessions terminated."));
    }

    // ── 7. Audit Logs & Dashboard Stats (Feature 11 & 12) ────────────────────

    @GetMapping("/logs")
    public ResponseEntity<List<SecurityLog>> getSecurityLogs() {
        return ResponseEntity.ok(securityLogRepository.findAll());
    }

    @GetMapping("/current-device")
    public ResponseEntity<?> getCurrentAdminDevice(HttpServletRequest request) {
        String sessionId = contextCollectorService.extractSessionId(request);
        String username = resolveAdminUsername(request);
        String deviceStatus = "UNKNOWN";
        String deviceInfo = "Unknown";
        String fingerprint = "";

        if (sessionId != null) {
            var opt = sessionService.validateSession(sessionId);
            if (opt.isPresent()) {
                Session s = opt.get();
                fingerprint = s.getDeviceFingerprint();
                deviceInfo = s.getDeviceInfo();
                if (fingerprint != null && !fingerprint.isBlank()) {
                    deviceStatus = deviceRepository
                            .findByUser_UsernameAndDeviceFingerprint(username, fingerprint)
                            .map(Device::getStatus)
                            .orElse("UNREGISTERED");
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "username", username,
                "deviceStatus", deviceStatus,
                "deviceTrusted", "TRUSTED".equalsIgnoreCase(deviceStatus),
                "deviceFingerprint", fingerprint != null ? fingerprint : "",
                "deviceInfo", deviceInfo != null ? deviceInfo : ""
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStats> getDashboardStats(HttpServletRequest request) {
        AdminStats stats = new AdminStats();

        List<User> users = userRepository.findAll();
        stats.setTotalUsers(users.size());
        stats.setActiveUsers(users.stream().filter(u -> !u.isLocked() && !"LOCKED".equalsIgnoreCase(u.getStatus())).count());
        stats.setLockedUsers(users.stream().filter(u -> u.isLocked() || "LOCKED".equalsIgnoreCase(u.getStatus())).count());

        List<Device> devices = deviceRepository.findAll();
        stats.setTotalDevices(devices.size());
        stats.setTrustedDevices(devices.stream().filter(d -> "TRUSTED".equalsIgnoreCase(d.getStatus())).count());
        stats.setUnregisteredDevices(devices.stream().filter(d -> "UNREGISTERED".equalsIgnoreCase(d.getStatus())).count());
        stats.setBlockedDevices(devices.stream().filter(d -> "BLOCKED".equalsIgnoreCase(d.getStatus())).count());

        stats.setActiveSessions(sessionService.getActiveSessions().size());

        List<NetworkRule> rules = networkService.getAllRules();
        stats.setTotalNetworkRules(rules.size());
        stats.setOfficeNetworks(rules.stream().filter(r -> "OFFICE".equalsIgnoreCase(r.getClassification())).count());
        stats.setHomeNetworks(rules.stream().filter(r -> "HOME".equalsIgnoreCase(r.getClassification())).count());
        stats.setBlockedNetworks(rules.stream().filter(r -> "BLOCKED".equalsIgnoreCase(r.getClassification())).count());

        List<AccessPolicy> policies = accessPolicyRepository.findAll();
        stats.setTotalPolicies(policies.size());
        stats.setAllowPolicies(policies.stream().filter(p -> "ALLOW".equalsIgnoreCase(p.getEffect())).count());
        stats.setDenyPolicies(policies.stream().filter(p -> "DENY".equalsIgnoreCase(p.getEffect())).count());

        List<SecurityLog> logs = securityLogRepository.findAll();
        stats.setTotalSecurityLogs(logs.size());
        stats.setCriticalSecurityEvents(logs.stream().filter(l -> "HIGH".equalsIgnoreCase(l.getSeverity()) || "CRITICAL".equalsIgnoreCase(l.getSeverity())).count());
        stats.setAllowDecisions(logs.stream().filter(l -> "PEP_ALLOW".equalsIgnoreCase(l.getEventType())).count());
        stats.setDenyDecisions(logs.stream().filter(l -> "PEP_DENY".equalsIgnoreCase(l.getEventType())).count());

        // Resolve admin's current device status
        String sessionId = contextCollectorService.extractSessionId(request);
        String username = resolveAdminUsername(request);
        if (sessionId != null) {
            var opt = sessionService.validateSession(sessionId);
            if (opt.isPresent()) {
                String fp = opt.get().getDeviceFingerprint();
                String status = (fp != null && !fp.isBlank())
                        ? deviceRepository.findByUser_UsernameAndDeviceFingerprint(username, fp).map(Device::getStatus).orElse("UNREGISTERED")
                        : "UNREGISTERED";
                stats.setCurrentDeviceStatus(status);
            }
        }

        return ResponseEntity.ok(stats);
    }

    private String resolveAdminUsername(HttpServletRequest request) {
        String sessionId = contextCollectorService.extractSessionId(request);
        if (sessionId != null) {
            var opt = sessionService.validateSession(sessionId);
            if (opt.isPresent() && opt.get().getUser() != null) {
                return opt.get().getUser().getUsername();
            }
        }
        return "ADMIN";
    }
}
