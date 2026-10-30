package com.ztna.service;

import com.ztna.dto.AdminStats;
import com.ztna.dto.ContextReportResponse;
import com.ztna.model.ContextSnapshot;
import com.ztna.model.Device;
import com.ztna.model.SecurityLog;
import com.ztna.model.User;
import com.ztna.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    private final ContextSnapshotRepository contextSnapshotRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final SessionRepository sessionRepository;
    private final SecurityLogRepository securityLogRepository;
    private final NetworkRuleRepository networkRuleRepository;
    private final AccessPolicyRepository accessPolicyRepository;
    private final LoginAttemptService loginAttemptService;
    private final NetworkAddressRepository networkAddressRepository;

    public ReportService(ContextSnapshotRepository contextSnapshotRepository,
            UserRepository userRepository,
            DeviceRepository deviceRepository,
            SessionRepository sessionRepository,
            SecurityLogRepository securityLogRepository,
            NetworkRuleRepository networkRuleRepository,
            AccessPolicyRepository accessPolicyRepository,
            LoginAttemptService loginAttemptService,
            NetworkAddressRepository networkAddressRepository) {
        this.contextSnapshotRepository = contextSnapshotRepository;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.sessionRepository = sessionRepository;
        this.securityLogRepository = securityLogRepository;
        this.networkRuleRepository = networkRuleRepository;
        this.accessPolicyRepository = accessPolicyRepository;
        this.loginAttemptService = loginAttemptService;
        this.networkAddressRepository = networkAddressRepository;
    }

    @Transactional(readOnly = true)
    public ContextReportResponse buildContextReport(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null)
            return null;

        ContextSnapshot ctx = contextSnapshotRepository
                .findTopByUsernameOrderByCapturedAtDesc(username)
                .orElse(null);

        // Also check if there's an active or most recent session for this user
        var userSessions = sessionRepository.findByUser_Username(username);
        var activeSession = userSessions.stream()
                .filter(com.ztna.model.Session::isActive)
                .findFirst()
                .orElse(userSessions.isEmpty() ? null : userSessions.get(userSessions.size() - 1));

        ContextReportResponse report = new ContextReportResponse();
        report.setUsername(username);
        report.setRole(user.getRole());
        report.setAccountStatus(user.isLocked() ? "LOCKED" : user.getStatus());
        report.setFailedLoginAttempts(loginAttemptService.getFailedAttempts(username));

        // Prioritize ContextSnapshot; if not present or session is newer, populate from
        // session & devices
        if (ctx != null) {
            report.setIpAddress(ctx.getIpAddress());
            report.setNetworkClassification(ctx.getNetworkClassification());
            report.setDeviceInfo(ctx.getDeviceInfo());
            report.setDeviceStatus(ctx.getDeviceStatus());
            report.setLastSeen(ctx.getCapturedAt());
        } else if (activeSession != null) {
            report.setIpAddress(activeSession.getIpAddress() != null ? activeSession.getIpAddress() : "127.0.0.1");
            report.setNetworkClassification(
                    activeSession.getNetworkClassification() != null ? activeSession.getNetworkClassification()
                            : "UNKNOWN");
            report.setDeviceInfo(activeSession.getDeviceInfo() != null ? activeSession.getDeviceInfo() : "Unknown");

            // Look up device status for this session
            String devStatus = "UNREGISTERED";
            if (activeSession.getDeviceFingerprint() != null && !activeSession.getDeviceFingerprint().isBlank()) {
                devStatus = deviceRepository
                        .findByUser_UsernameAndDeviceFingerprint(username, activeSession.getDeviceFingerprint())
                        .map(Device::getStatus)
                        .orElse("UNREGISTERED");
            }
            report.setDeviceStatus(devStatus);
            report.setLastSeen(activeSession.getLastActivity() != null ? activeSession.getLastActivity()
                    : activeSession.getLoginTime());
        } else {
            // Check if user has registered devices or network addresses
            var devList = deviceRepository.findByUser_Username(username);
            String devStatus = devList.isEmpty() ? "UNREGISTERED" : devList.get(0).getStatus();
            String devInfo = devList.isEmpty() ? "Unknown" : devList.get(0).getDeviceName();

            var addrList = networkAddressRepository.findByUser_Username(username);
            String ip = (addrList != null && !addrList.isEmpty()) ? addrList.get(0).getIpAddress() : "127.0.0.1";
            String netClass = (addrList != null && !addrList.isEmpty()) ? addrList.get(0).getLabel() : "UNKNOWN";

            report.setIpAddress(ip);
            report.setNetworkClassification(netClass);
            report.setDeviceInfo(devInfo);
            report.setDeviceStatus(devStatus);
            report.setLastSeen(user.getLastLoginAt() != null ? user.getLastLoginAt() : LocalDateTime.now());
        }

        List<SecurityLog> logs = securityLogRepository.findByUsernameOrderByEventTimeDesc(username);
        if (!logs.isEmpty()) {
            report.setLastDecision(logs.stream()
                    .filter(l -> l.getEventType().startsWith("PEP_"))
                    .map(l -> l.getEventType().replace("PEP_", ""))
                    .findFirst().orElse("ALLOW"));
            report.setRecentAuditEvents(logs.stream().limit(10).toList());
        } else {
            report.setLastDecision("ALLOW");
            report.setRecentAuditEvents(List.of());
        }

        return report;
    }

    public AdminStats buildDashboardStats() {
        AdminStats stats = new AdminStats();

        List<User> users = userRepository.findAll();
        stats.setTotalUsers(users.size());
        stats.setActiveUsers(
                users.stream().filter(u -> !u.isLocked() && !"LOCKED".equalsIgnoreCase(u.getStatus())).count());
        stats.setLockedUsers(
                users.stream().filter(u -> u.isLocked() || "LOCKED".equalsIgnoreCase(u.getStatus())).count());

        List<Device> devices = deviceRepository.findAll();
        stats.setTotalDevices(devices.size());
        stats.setTrustedDevices(devices.stream().filter(d -> "TRUSTED".equalsIgnoreCase(d.getStatus())).count());
        stats.setUnregisteredDevices(
                devices.stream().filter(d -> "UNREGISTERED".equalsIgnoreCase(d.getStatus())).count());
        stats.setBlockedDevices(devices.stream().filter(d -> "BLOCKED".equalsIgnoreCase(d.getStatus())).count());

        stats.setActiveSessions(sessionRepository.findByActiveTrue().size());

        var rules = networkRuleRepository.findAll();
        stats.setTotalNetworkRules(rules.size());
        stats.setOfficeNetworks(rules.stream().filter(r -> "OFFICE".equalsIgnoreCase(r.getClassification())).count());
        stats.setHomeNetworks(rules.stream().filter(r -> "HOME".equalsIgnoreCase(r.getClassification())).count());
        stats.setBlockedNetworks(rules.stream().filter(r -> "BLOCKED".equalsIgnoreCase(r.getClassification())).count());

        var policies = accessPolicyRepository.findAll();
        stats.setTotalPolicies(policies.size());
        stats.setAllowPolicies(policies.stream().filter(p -> "ALLOW".equalsIgnoreCase(p.getEffect())).count());
        stats.setDenyPolicies(policies.stream().filter(p -> "DENY".equalsIgnoreCase(p.getEffect())).count());

        List<SecurityLog> logs = securityLogRepository.findAll();
        stats.setTotalSecurityLogs(logs.size());
        stats.setCriticalSecurityEvents(logs.stream()
                .filter(l -> "HIGH".equalsIgnoreCase(l.getSeverity()) || "CRITICAL".equalsIgnoreCase(l.getSeverity()))
                .count());
        stats.setAllowDecisions(logs.stream().filter(l -> "PEP_ALLOW".equalsIgnoreCase(l.getEventType())).count());
        stats.setDenyDecisions(logs.stream().filter(l -> "PEP_DENY".equalsIgnoreCase(l.getEventType())).count());

        return stats;
    }
}
