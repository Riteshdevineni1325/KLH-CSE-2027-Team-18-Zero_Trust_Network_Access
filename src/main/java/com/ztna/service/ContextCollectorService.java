package com.ztna.service;

import com.ztna.dto.PolicyEvaluationResult;
import com.ztna.model.ContextSnapshot;
import com.ztna.model.User;
import com.ztna.repository.ContextSnapshotRepository;
import com.ztna.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Context Collector Service.
 * Implements Feature 6: Collects relevant information such as user identity, role,
 * device status, IP/network classification, time, session state, and requested resource.
 * Feeds the assembled context directly to PolicyDecisionService (PDP).
 */
@Service
public class ContextCollectorService {

    private final ContextSnapshotRepository contextSnapshotRepository;
    private final PolicyDecisionService policyDecisionService;
    private final LoginAttemptService loginAttemptService;
    private final DeviceService deviceService;
    private final NetworkService networkService;
    private final UserRepository userRepository;

    public ContextCollectorService(
            ContextSnapshotRepository contextSnapshotRepository,
            PolicyDecisionService policyDecisionService,
            LoginAttemptService loginAttemptService,
            DeviceService deviceService,
            NetworkService networkService,
            UserRepository userRepository) {
        this.contextSnapshotRepository = contextSnapshotRepository;
        this.policyDecisionService = policyDecisionService;
        this.loginAttemptService = loginAttemptService;
        this.deviceService = deviceService;
        this.networkService = networkService;
        this.userRepository = userRepository;
    }

    /**
     * Builds and persists context snapshot, then evaluates access for a protected resource request.
     */
    public PolicyEvaluationResult collectAndEvaluate(String username, String resourceName, HttpServletRequest request) {
        ContextSnapshot snapshot = buildFromRequest(username, resourceName, request);
        contextSnapshotRepository.save(snapshot);

        User user = userRepository.findByUsername(username).orElse(null);
        String role = user != null ? user.getRole() : "USER";
        boolean locked = user != null && (user.isLocked() || "LOCKED".equalsIgnoreCase(user.getStatus()));

        return policyDecisionService.evaluateAccess(
                username,
                role,
                snapshot.getNetworkClassification(),
                snapshot.getDeviceStatus(),
                locked,
                resourceName
        );
    }

    /**
     * Constructs a ContextSnapshot from request metadata.
     */
    public ContextSnapshot buildFromRequest(String username, String resourceName, HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String customDevice = request.getHeader("X-Device-Name");
        String rawDevice = (customDevice != null && !customDevice.isBlank()) ? customDevice : (userAgent != null ? userAgent : "Unknown Device");

        // Resolve device
        String os = deviceService.extractOs(userAgent);
        String browser = deviceService.extractBrowser(userAgent);
        String fingerprint = deviceService.computeFingerprint(username, rawDevice, os, browser);
        String deviceStatus = deviceService.resolveDeviceStatus(username, rawDevice, os, browser, clientIp);

        // Classify network IP
        String networkClass = networkService.classifyIpForUser(username, clientIp);

        ContextSnapshot ctx = new ContextSnapshot();
        ctx.setUsername(username);
        ctx.setSessionId(extractSessionId(request));
        ctx.setIpAddress(clientIp);
        ctx.setNetworkClassification(networkClass);
        ctx.setDeviceFingerprint(fingerprint);
        ctx.setDeviceInfo(rawDevice);
        ctx.setDeviceStatus(deviceStatus);
        ctx.setRequestedResource(resourceName);
        ctx.setLoginTime(LocalDateTime.now());
        ctx.setHourOfDay(LocalDateTime.now().getHour());
        ctx.setFailedAttempts(loginAttemptService.getFailedAttempts(username));
        ctx.setSessionActivity("NORMAL");
        ctx.setLocationHint("Network: " + networkClass + " | Device: " + deviceStatus);
        ctx.setCapturedAt(LocalDateTime.now());

        return ctx;
    }

    public String extractClientIp(HttpServletRequest request) {
        // 1. Standard reverse-proxy header (nginx, AWS ALB, etc.)
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            String ip = fwd.split(",")[0].trim();
            if (isRoutableIp(ip)) return ip;
        }

        // 2. Single-IP proxy header
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            String ip = real.trim();
            if (isRoutableIp(ip)) return ip;
        }

        // 3. Browser-detected real LAN/WiFi IP sent by frontend via WebRTC.
        //    Only accepted if it is a real routable address (not loopback or link-local).
        String clientReal = request.getHeader("X-Client-Real-IP");
        if (clientReal != null && !clientReal.isBlank() && isRoutableIp(clientReal)) {
            return clientReal.trim();
        }

        // 4. Raw socket address
        String remote = request.getRemoteAddr();
        if (remote != null) {
            remote = remote.trim();
            if (isRoutableIp(remote)) {
                return remote;
            }
        }

        // 5. If the request originates from localhost (e.g. ::1 or 127.0.0.1 in local development / testing),
        //    introspect the active physical network interfaces to resolve the actual host LAN IP.
        String lanIp = resolveHostLanIp();
        if (lanIp != null && !lanIp.isBlank()) {
            return lanIp;
        }

        return "127.0.0.1";
    }

    /**
     * Inspects host network interfaces to resolve the machine's primary site-local (LAN/WiFi) IPv4 address.
     * Skips loopback, inactive, virtual, and non-site-local interfaces.
     */
    public static String resolveHostLanIp() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) return null;

            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface networkInterface = interfaces.nextElement();

                // Skip loopback (127.0.0.1), inactive, and virtual/point-to-point interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp() || networkInterface.isPointToPoint()) {
                    continue;
                }

                java.util.Enumeration<java.net.InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();

                    // Filter out IPv6 addresses and non-LAN addresses; select standard IPv4 site-local LAN address
                    if (address instanceof java.net.Inet4Address && address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (java.net.SocketException ignored) {
            // Fall back gracefully if socket inspection fails
        }
        return null;
    }

    /**
     * Returns true for a routable IPv4 address — i.e. not loopback (127.x),
     * not link-local (169.254.x.x), not IPv6 loopback (::1 / 0:0:...).
     */
    private boolean isRoutableIp(String ip) {
        if (ip == null || ip.isBlank()) return false;
        String t = ip.trim();
        return !t.startsWith("127.")
            && !t.startsWith("169.254.")
            && !t.equals("::1")
            && !t.startsWith("0:0:0:0:0:0:0:1")
            && !t.equals("0:0:0:0:0:0:0:0");
    }

    public String extractSessionId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        String xSid = request.getHeader("X-Session-Id");
        if (xSid != null && !xSid.isBlank()) return xSid.trim();
        return request.getParameter("sessionId");
    }
}
