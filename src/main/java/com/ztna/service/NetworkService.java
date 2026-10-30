package com.ztna.service;

import com.ztna.model.IpHistory;
import com.ztna.model.NetworkAddress;
import com.ztna.model.NetworkRule;
import com.ztna.model.User;
import com.ztna.repository.IpHistoryRepository;
import com.ztna.repository.NetworkAddressRepository;
import com.ztna.repository.NetworkRuleRepository;
import com.ztna.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Network Service.
 *
 * IP classification uses two layers:
 *   1. Global NetworkRule table (admin-configured CIDR/IP rules) — OFFICE/HOME/BLOCKED
 *   2. Per-user NetworkAddress table (auto-assigned on registration & login)
 *
 * Auto-labelling rules:
 *   - First IP seen at registration  → OFFICE (TRUSTED)
 *   - Any subsequent different IP    → HOME   (TRUSTED)
 *   - Admin-blocked IP               → BLOCKED
 */
@Service
public class NetworkService {

    private final NetworkRuleRepository networkRuleRepository;
    private final NetworkAddressRepository networkAddressRepository;
    private final IpHistoryRepository ipHistoryRepository;
    private final UserRepository userRepository;
    private final SecurityLogService securityLogService;

    public NetworkService(NetworkRuleRepository networkRuleRepository,
                          NetworkAddressRepository networkAddressRepository,
                          IpHistoryRepository ipHistoryRepository,
                          UserRepository userRepository,
                          SecurityLogService securityLogService) {
        this.networkRuleRepository = networkRuleRepository;
        this.networkAddressRepository = networkAddressRepository;
        this.ipHistoryRepository = ipHistoryRepository;
        this.userRepository = userRepository;
        this.securityLogService = securityLogService;
    }

    // ── Classification ────────────────────────────────────────────────────────

    /**
     * Classifies an IP against global NetworkRule entries.
     * Priority: BLOCKED > OFFICE > HOME > UNKNOWN
     */
    public String classifyIp(String ipAddress) {
        if (!isUsableIp(ipAddress)) return "UNKNOWN";

        List<NetworkRule> rules = networkRuleRepository.findByActiveTrue();

        for (NetworkRule rule : rules) {
            if ("BLOCKED".equalsIgnoreCase(rule.getClassification())
                    && matchesIpOrCidr(ipAddress, rule.getIpOrCidr())) {
                return "BLOCKED";
            }
        }
        for (NetworkRule rule : rules) {
            if ("OFFICE".equalsIgnoreCase(rule.getClassification())
                    && matchesIpOrCidr(ipAddress, rule.getIpOrCidr())) {
                return "OFFICE";
            }
        }
        for (NetworkRule rule : rules) {
            if ("HOME".equalsIgnoreCase(rule.getClassification())
                    && matchesIpOrCidr(ipAddress, rule.getIpOrCidr())) {
                return "HOME";
            }
        }
        return "UNKNOWN";
    }

    /**
     * Classifies an IP for a specific user.
     *
     * Lookup order:
     *   1. Global BLOCKED rule  — always wins
     *   2. Per-user NetworkAddress label  — OFFICE/HOME set automatically
     *   3. Global NetworkRule match       — admin-configured subnet rules
     *   4. UNKNOWN fallback
     */
    public String classifyIpForUser(String username, String ipAddress) {
        if (!isUsableIp(ipAddress)) return "UNKNOWN";

        // Global BLOCKED always takes precedence
        String global = classifyIp(ipAddress);
        if ("BLOCKED".equals(global)) return "BLOCKED";

        // Per-user label (set by recordRegistrationIp / observeIp)
        if (username != null) {
            Optional<NetworkAddress> addr =
                    networkAddressRepository.findByUser_UsernameAndIpAddress(username, ipAddress);
            if (addr.isPresent()) {
                NetworkAddress na = addr.get();
                if ("BLOCKED".equalsIgnoreCase(na.getStatus())
                        || "BLOCKED".equalsIgnoreCase(na.getLabel())) {
                    return "BLOCKED";
                }
                String label = na.getLabel();
                if ("OFFICE".equalsIgnoreCase(label)) return "OFFICE";
                if ("HOME".equalsIgnoreCase(label))   return "HOME";
            } else {
                // If the user already has an established OFFICE IP and this new IP is unknown to them,
                // auto-direct this second unknown IP as HOME (unless globally blocked).
                boolean hasOffice = networkAddressRepository
                        .findByUser_Username(username)
                        .stream()
                        .anyMatch(a -> "OFFICE".equalsIgnoreCase(a.getLabel()));
                if (hasOffice) {
                    return "HOME";
                }
            }
        }

        // Fall back to global rule result
        return global;
    }

    // ── IP auto-labelling ─────────────────────────────────────────────────────

    /**
     * Called at registration time.
     * Records the registration IP as OFFICE/TRUSTED — the user's canonical origin.
     * Skipped silently if the IP is loopback/unusable or OFFICE already set.
     */
    @Transactional
    public void recordRegistrationIp(String username, String ipAddress) {
        if (!isUsableIp(ipAddress)) return;

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return;

        boolean alreadyHasOffice = networkAddressRepository
                .findByUser_Username(username)
                .stream()
                .anyMatch(na -> "OFFICE".equalsIgnoreCase(na.getLabel()));

        if (alreadyHasOffice) return;

        NetworkAddress na = networkAddressRepository
                .findByUser_UsernameAndIpAddress(username, ipAddress)
                .orElseGet(() -> {
                    NetworkAddress n = new NetworkAddress();
                    n.setUser(user);
                    n.setIpAddress(ipAddress);
                    n.setFirstSeen(LocalDateTime.now());
                    n.setAddedAt(LocalDateTime.now());
                    return n;
                });

        na.setLabel("OFFICE");
        na.setZone("INTERNAL");
        na.setStatus("TRUSTED");
        na.setLastSeen(LocalDateTime.now());
        networkAddressRepository.save(na);

        securityLogService.logEvent(username, "IP_OFFICE_REGISTERED", ipAddress,
                "Registration IP recorded as OFFICE for user: " + username, "INFO");
    }

    /**
     * Called on every login.
     * - Known IP → update lastSeen only.
     * - New IP, no OFFICE yet → label as OFFICE.
     * - New IP, OFFICE already exists → label as HOME.
     * Always appends an immutable row to ip_history.
     */
    @Transactional
    public NetworkAddress observeIp(String username, String ipAddress) {
        if (!isUsableIp(ipAddress)) return null;

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return null;

        Optional<NetworkAddress> existing =
                networkAddressRepository.findByUser_UsernameAndIpAddress(username, ipAddress);

        NetworkAddress na;

        if (existing.isPresent()) {
            na = existing.get();
        } else {
            na = new NetworkAddress();
            na.setUser(user);
            na.setIpAddress(ipAddress);
            na.setFirstSeen(LocalDateTime.now());
            na.setAddedAt(LocalDateTime.now());

            boolean hasOffice = networkAddressRepository
                    .findByUser_Username(username)
                    .stream()
                    .anyMatch(a -> "OFFICE".equalsIgnoreCase(a.getLabel()));

            if (!hasOffice) {
                na.setLabel("OFFICE");
                na.setZone("INTERNAL");
                na.setStatus("TRUSTED");
            } else {
                na.setLabel("HOME");
                na.setZone("EXTERNAL");
                na.setStatus("TRUSTED");
                securityLogService.logEvent(username, "IP_HOME_REGISTERED", ipAddress,
                        "New IP auto-labelled as HOME for user: " + username, "INFO");
            }

            securityLogService.logEvent(username, "IP_FIRST_SEEN", ipAddress,
                    "New IP observed [" + na.getLabel() + "] for user: " + username, "WARN");
        }

        na.setLastSeen(LocalDateTime.now());
        networkAddressRepository.save(na);

        ipHistoryRepository.save(new IpHistory(user, ipAddress, "LOGIN"));
        return na;
    }

    // ── Admin management API ─────────────────────────────────────────────────

    public List<NetworkRule> getAllRules() {
        return networkRuleRepository.findAll();
    }

    @Transactional
    public NetworkRule saveRule(String ipOrCidr, String classification, String description) {
        NetworkRule rule = networkRuleRepository.findByIpOrCidr(ipOrCidr)
                .orElse(new NetworkRule(ipOrCidr, classification, description));
        rule.setClassification(classification.toUpperCase());
        rule.setDescription(description);
        rule.setActive(true);
        return networkRuleRepository.save(rule);
    }

    @Transactional
    public void deleteRule(Long id) {
        networkRuleRepository.deleteById(id);
    }

    public List<NetworkAddress> getUserAddresses(String username) {
        return networkAddressRepository.findByUser_Username(username);
    }

    // ── IP / CIDR matching ────────────────────────────────────────────────────

    public static boolean matchesIpOrCidr(String clientIp, String ruleIpOrCidr) {
        if (clientIp == null || ruleIpOrCidr == null) return false;
        String c = clientIp.trim(), r = ruleIpOrCidr.trim();
        if (c.equalsIgnoreCase(r)) return true;
        if (r.contains("/")) {
            try { return isIpInCidr(c, r); } catch (Exception e) { return false; }
        }
        return false;
    }

    private static boolean isIpInCidr(String ip, String cidr) {
        String[] parts = cidr.split("/");
        if (parts.length != 2) return false;
        int prefix = Integer.parseInt(parts[1]);
        if (prefix < 0 || prefix > 32) return false;
        long ipLong     = ipToLong(ip);
        long subnetLong = ipToLong(parts[0]);
        if (ipLong == -1 || subnetLong == -1) return false;
        long mask = (prefix == 0) ? 0 : (~0L << (32 - prefix)) & 0xFFFFFFFFL;
        return (ipLong & mask) == (subnetLong & mask);
    }

    private static long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        if (octets.length != 4) return -1;
        try {
            long result = 0;
            for (int i = 0; i < 4; i++) {
                long octet = Long.parseLong(octets[i]);
                if (octet < 0 || octet > 255) return -1;
                result |= (octet << (24 - (8 * i)));
            }
            return result & 0xFFFFFFFFL;
        } catch (NumberFormatException e) { return -1; }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true for a usable IPv4 address — not null, not loopback, not link-local.
     */
    public boolean isUsableIp(String ip) {
        if (ip == null || ip.isBlank() || "UNKNOWN".equalsIgnoreCase(ip)) return false;
        String t = ip.trim();
        return !t.startsWith("127.")
            && !t.startsWith("169.254.")
            && !t.equals("::1")
            && !t.startsWith("0:0:0:0:0:0:0:1")
            && !t.equals("0:0:0:0:0:0:0:0");
    }
}
