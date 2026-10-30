package com.ztna.service;

import com.ztna.model.Device;
import com.ztna.model.User;
import com.ztna.repository.DeviceRepository;
import com.ztna.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final SecurityLogService securityLogService;

    public DeviceService(DeviceRepository deviceRepository,
                         UserRepository userRepository,
                         SecurityLogService securityLogService) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.securityLogService = securityLogService;
    }

    // ── Fingerprinting ────────────────────────────────────────────────────────

    /**
     * Derives a stable hex fingerprint from the device attributes.
     * sha256(username|deviceName|os|browser) — first 16 hex chars used.
     */
    public String computeFingerprint(String username, String deviceName,
                                      String os, String browser) {
        String raw = (username + "|" + safeStr(deviceName) + "|"
                + safeStr(os) + "|" + safeStr(browser)).toLowerCase();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.substring(0, 16); // 16 hex chars = 8 bytes, sufficient for fingerprint
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in Java; fall back to simple hash if not
            return Integer.toHexString(raw.hashCode());
        }
    }

    /**
     * Derives a fingerprint from a raw User-Agent string by extracting OS and
     * browser hints. Used during login when no explicit device info is supplied.
     */
    public String computeFingerprintFromUserAgent(String username, String userAgent) {
        String os = extractOs(userAgent);
        String browser = extractBrowser(userAgent);
        return computeFingerprint(username, userAgent, os, browser);
    }

    // ── Registration ─────────────────────────────────────────────────────────

    /**
     * Registers a device for a user. If a device with the same fingerprint
     * already exists it is updated (last-seen, status if explicitly provided).
     */
    @Transactional
    public Device registerDevice(String username, String deviceName,
                                  String deviceType, String os, String browser,
                                  String ipAddress, String status) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        String fingerprint = computeFingerprint(username, deviceName, os, browser);

        Optional<Device> existing =
                deviceRepository.findByUser_UsernameAndDeviceFingerprint(username, fingerprint);

        Device device;
        if (existing.isPresent()) {
            device = existing.get();
            device.setLastSeen(LocalDateTime.now());
            // Update status only if explicitly given
            if (status != null && !status.isBlank()) {
                device.setStatus(status.toUpperCase());
            }
        } else {
            device = new Device();
            device.setUser(user);
            device.setDeviceFingerprint(fingerprint);
            device.setDeviceName(deviceName != null ? deviceName : "UNKNOWN");
            device.setDeviceType(deviceType);
            device.setOperatingSystem(os);
            device.setBrowser(browser);
            device.setStatus(status != null && !status.isBlank()
                    ? status.toUpperCase() : "UNREGISTERED");
            device.setFirstSeen(LocalDateTime.now());
            device.setLastSeen(LocalDateTime.now());
            device.setRegisteredAt(LocalDateTime.now());

            securityLogService.logEvent(
                    username,
                    "DEVICE_REGISTERED",
                    ipAddress != null ? ipAddress : "UNKNOWN",
                    "New device registered: " + device.getDeviceName()
                    + " [" + fingerprint + "]",
                    "INFO"
            );
        }

        return deviceRepository.save(device);
    }

    // ── Auto-observe (called on every login; non-blocking) ────────────────────

    /**
     * Observes a device seen during login/access.
     *
     * Lookup order:
     *   1. Exact fingerprint match  — fastest, used once device is registered via code.
     *   2. OS + browser match       — fallback for seeded/manually-created device records
     *                                 whose fingerprints don't match the computed hash.
     *
     * Returns the device status: TRUSTED | UNREGISTERED | BLOCKED.
     * Auto-creates an UNREGISTERED entry if the device is completely new.
     */
    @Transactional
    public String observeAndResolveStatus(String username, String deviceInfo,
                                           String ipAddress) {
        if (username == null || username.isBlank()) return "UNREGISTERED";
        if (deviceInfo == null || deviceInfo.isBlank() || "UNKNOWN".equalsIgnoreCase(deviceInfo)) {
            return "UNREGISTERED";
        }

        String os          = extractOs(deviceInfo);
        String browser     = extractBrowser(deviceInfo);
        String fingerprint = computeFingerprint(username, deviceInfo, os, browser);

        // 1. Exact fingerprint match
        Optional<Device> exact =
                deviceRepository.findByUser_UsernameAndDeviceFingerprint(username, fingerprint);
        if (exact.isPresent()) {
            Device d = exact.get();
            d.setLastSeen(LocalDateTime.now());
            deviceRepository.save(d);
            return d.getStatus();
        }

        // 2. Fuzzy fallback: match by OS + browser against all devices for this user.
        //    Covers seeded records whose fingerprints were set manually.
        List<Device> userDevices = deviceRepository.findByUser_Username(username);
        for (Device d : userDevices) {
            boolean osMatch      = os.equalsIgnoreCase(d.getOperatingSystem());
            boolean browserMatch = browser.equalsIgnoreCase(d.getBrowser());
            if (osMatch && browserMatch) {
                // Upgrade the stored fingerprint so future lookups use exact match
                d.setDeviceFingerprint(fingerprint);
                d.setLastSeen(LocalDateTime.now());
                deviceRepository.save(d);
                return d.getStatus();
            }
        }

        // 3. Brand new device — TRUSTED if this is the user's first device, UNREGISTERED otherwise
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            boolean isFirstDevice = deviceRepository.findByUser_Username(username).isEmpty();
            String autoStatus = isFirstDevice ? "TRUSTED" : "UNREGISTERED";

            Device d = new Device();
            d.setUser(user);
            d.setDeviceFingerprint(fingerprint);
            d.setDeviceName(deviceInfo.length() > 120 ? deviceInfo.substring(0, 120) : deviceInfo);
            d.setDeviceType(guessDeviceType(deviceInfo));
            d.setOperatingSystem(os);
            d.setBrowser(browser);
            d.setStatus(autoStatus);
            d.setFirstSeen(LocalDateTime.now());
            d.setLastSeen(LocalDateTime.now());
            deviceRepository.save(d);

            securityLogService.logEvent(username, "DEVICE_FIRST_SEEN",
                    ipAddress != null ? ipAddress : "UNKNOWN",
                    "Device auto-registered as " + autoStatus + ": " + d.getDeviceName()
                    + " [" + fingerprint + "]", "INFO");

            return autoStatus;
        }

        return "UNREGISTERED";
    }

    public Device setDeviceStatus(Long deviceId, String status) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceId));

        String normalised = status.toUpperCase();
        device.setStatus(normalised);

        if ("BLOCKED".equals(normalised)) {
            securityLogService.logEvent(
                    device.getUser().getUsername(),
                    "DEVICE_BLOCKED",
                    "SYSTEM",
                    "Device blocked by admin: " + device.getDeviceName()
                    + " [" + device.getDeviceFingerprint() + "]",
                    "HIGH"
            );
        }

        return deviceRepository.save(device);
    }

    public void deleteDevice(Long deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw new RuntimeException("Device not found: " + deviceId);
        }
        deviceRepository.deleteById(deviceId);
    }

    public List<Device> getDevicesByUsername(String username) {
        return deviceRepository.findByUser_Username(username);
    }

    public Device getDeviceById(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Device not found: " + id));
    }

    @Transactional
    public String resolveDeviceStatus(String username, String deviceInfo,
                                       String os, String browser, String ipAddress) {
        if (username == null || username.isBlank()) return "UNREGISTERED";
        if (deviceInfo == null || deviceInfo.isBlank() || "UNKNOWN".equalsIgnoreCase(deviceInfo)) {
            return "UNREGISTERED";
        }

        // Use the already-extracted os/browser rather than re-parsing deviceInfo,
        // which may be a simplified label rather than a raw User-Agent string.
        String fingerprint = computeFingerprint(username, deviceInfo, os, browser);

        // 1. Exact fingerprint match
        Optional<Device> exact =
                deviceRepository.findByUser_UsernameAndDeviceFingerprint(username, fingerprint);
        if (exact.isPresent()) {
            Device d = exact.get();
            d.setLastSeen(LocalDateTime.now());
            deviceRepository.save(d);
            return d.getStatus();
        }

        // 2. Fuzzy fallback: match on OS + browser across the user's registered devices.
        //    This handles seeded/admin-created records whose fingerprints differ.
        List<Device> userDevices = deviceRepository.findByUser_Username(username);
        for (Device d : userDevices) {
            boolean osMatch      = os.equalsIgnoreCase(d.getOperatingSystem());
            boolean browserMatch = browser.equalsIgnoreCase(d.getBrowser());
            if (osMatch && browserMatch) {
                // Upgrade the stored fingerprint so future lookups hit the fast path
                d.setDeviceFingerprint(fingerprint);
                d.setLastSeen(LocalDateTime.now());
                deviceRepository.save(d);
                return d.getStatus();
            }
        }

        // 3. Brand new device — TRUSTED if this is the user's first device, UNREGISTERED otherwise
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            boolean isFirstDevice = deviceRepository.findByUser_Username(username).isEmpty();
            String autoStatus = isFirstDevice ? "TRUSTED" : "UNREGISTERED";

            Device d = new Device();
            d.setUser(user);
            d.setDeviceFingerprint(fingerprint);
            d.setDeviceName(deviceInfo.length() > 120 ? deviceInfo.substring(0, 120) : deviceInfo);
            d.setDeviceType(guessDeviceType(deviceInfo));
            d.setOperatingSystem(os);
            d.setBrowser(browser);
            d.setStatus(autoStatus);
            d.setFirstSeen(LocalDateTime.now());
            d.setLastSeen(LocalDateTime.now());
            deviceRepository.save(d);

            securityLogService.logEvent(username, "DEVICE_FIRST_SEEN",
                    ipAddress != null ? ipAddress : "UNKNOWN",
                    "Device auto-registered as " + autoStatus + ": " + d.getDeviceName()
                    + " [" + fingerprint + "]", "INFO");

            return autoStatus;
        }

        return "UNREGISTERED";
    }

    private String safeStr(String s) {
        return s == null ? "" : s.trim();
    }

    public String extractOs(String ua) {
        if (ua == null) return "Unknown";
        String l = ua.toLowerCase();
        if (l.contains("windows nt 10") || l.contains("windows nt 11")) return "Windows 11/10";
        if (l.contains("windows"))  return "Windows";
        if (l.contains("mac os x") || l.contains("macos")) return "macOS";
        if (l.contains("android"))  return "Android";
        if (l.contains("iphone") || l.contains("ipad")) return "iOS";
        if (l.contains("linux"))    return "Linux";
        return "Unknown";
    }

    public String extractBrowser(String ua) {
        if (ua == null) return "Unknown";
        String l = ua.toLowerCase();
        if (l.contains("edg/"))    return "Edge";
        if (l.contains("chrome/")) return "Chrome";
        if (l.contains("firefox/")) return "Firefox";
        if (l.contains("safari/") && !l.contains("chrome")) return "Safari";
        if (l.contains("opr/") || l.contains("opera")) return "Opera";
        return "Unknown";
    }

    private String guessDeviceType(String ua) {
        if (ua == null) return "Unknown";
        String l = ua.toLowerCase();
        if (l.contains("mobile") || l.contains("android") || l.contains("iphone")) return "Mobile";
        if (l.contains("tablet") || l.contains("ipad")) return "Tablet";
        return "Workstation";
    }
}
