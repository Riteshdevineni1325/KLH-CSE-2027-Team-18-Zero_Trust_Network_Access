package com.ztna.service;

import com.ztna.model.SuperRecoveryKey;
import com.ztna.repository.SuperRecoveryKeyRepository;
import com.ztna.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Super Key Recovery Service.
 * Implements Feature 3: Allows authorized administrators to unlock accounts using
 * a cryptographically secure, session-bound, short-lived (10 min), single-use Super Key.
 *
 * Security Requirements:
 * - Store ONLY its cryptographic hash (SHA-256) in PostgreSQL.
 * - NEVER log or persist the plaintext key.
 * - Bound to the active administrator session.
 * - Single-use enforcement.
 */
@Service
public class SuperKeyService {

    private static final int EXPIRATION_MINUTES = 10;

    private final SuperRecoveryKeyRepository superRecoveryKeyRepository;
    private final LoginAttemptService loginAttemptService;
    private final UserRepository userRepository;
    private final SecurityLogService securityLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    public SuperKeyService(SuperRecoveryKeyRepository superRecoveryKeyRepository,
                           LoginAttemptService loginAttemptService,
                           UserRepository userRepository,
                           SecurityLogService securityLogService) {
        this.superRecoveryKeyRepository = superRecoveryKeyRepository;
        this.loginAttemptService = loginAttemptService;
        this.userRepository = userRepository;
        this.securityLogService = securityLogService;
    }

    /**
     * Generates a single-use, session-bound 4-digit Super Key.
     * Target username is optional at generation time — pass "__PENDING__" when the
     * target is only known at redemption. The real target is stamped during redemption.
     * Persists ONLY the SHA-256 hash in PostgreSQL.
     * Returns the plaintext key once for administrator display.
     */
    @Transactional
    public String generateSuperKey(String adminSessionId, String adminUsername, String targetUsername) {
        // NOTE: No user existence check here — target may be "__PENDING__" when generated
        //       without a specific target. The target is validated at redemption time.

        // 1. Generate a 4-digit cryptographically secure string (0000 - 9999)
        String plaintextKey = String.format("%04d", secureRandom.nextInt(10_000));

        // 2. Compute SHA-256 hash
        String hashedKey = hashKey(plaintextKey);

        // 3. Persist in PostgreSQL
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);
        SuperRecoveryKey recoveryKey = new SuperRecoveryKey(
                adminSessionId,
                adminUsername,
                targetUsername,
                hashedKey,
                expiresAt
        );
        superRecoveryKeyRepository.save(recoveryKey);

        // 4. Audit Log (Note: plaintextKey is strictly excluded from logging)
        securityLogService.logEvent(
                adminUsername,
                "SUPER_KEY_GENERATED",
                "SYSTEM",
                "Admin generated short-lived recovery key for target '" + targetUsername +
                        "' (Expires in " + EXPIRATION_MINUTES + " min). Single-use only.",
                "HIGH",
                adminSessionId
        );

        return plaintextKey;
    }

    /**
     * Validates and redeems a Super Key to unlock a user account.
     *
     * Lookup strategy:
     *  1. Try an exact match on (targetUsername + adminSessionId) — covers keys that were
     *     generated with a specific target (original flow / tests).
     *  2. Fall back to session-only lookup — covers keys generated without a target
     *     (the no-username-at-generation UI flow, stored with "__PENDING__" placeholder).
     *
     * The real target username is stamped onto the record before marking it used.
     */
    @Transactional
    public boolean redeemSuperKeyAndUnlock(String adminSessionId, String adminUsername,
                                           String targetUsername, String candidateKey) {
        if (candidateKey == null || candidateKey.isBlank()) return false;

        LocalDateTime now = LocalDateTime.now();

        // 1. Try exact match (target username known at generation time)
        Optional<SuperRecoveryKey> keyOpt = superRecoveryKeyRepository
                .findTopByTargetUsernameAndAdminSessionIdAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        targetUsername, adminSessionId, now);

        // 2. Fall back to session-only (target was "__PENDING__" at generation time)
        if (keyOpt.isEmpty()) {
            keyOpt = superRecoveryKeyRepository
                    .findTopByAdminSessionIdAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                            adminSessionId, now);
        }

        if (keyOpt.isEmpty()) {
            securityLogService.logEvent(adminUsername, "SUPER_KEY_FAILED", "SYSTEM", adminSessionId,
                    "Super Key redemption failed: No valid active key for session / target: " + targetUsername, "WARN");
            return false;
        }

        SuperRecoveryKey recoveryKey = keyOpt.get();
        String candidateHash = hashKey(candidateKey.trim());

        if (!MessageDigest.isEqual(
                recoveryKey.getHashedKey().getBytes(StandardCharsets.UTF_8),
                candidateHash.getBytes(StandardCharsets.UTF_8))) {
            securityLogService.logEvent(adminUsername, "SUPER_KEY_FAILED", "SYSTEM", adminSessionId,
                    "Super Key redemption failed: Incorrect key for target: " + targetUsername, "HIGH");
            return false;
        }

        // Stamp the real target username (in case it was __PENDING__) and mark used
        recoveryKey.setTargetUsername(targetUsername);
        recoveryKey.setUsed(true);
        recoveryKey.setUsedAt(now);
        superRecoveryKeyRepository.save(recoveryKey);

        // Unlock the target user and reset attempts
        loginAttemptService.resetAttemptsByAdmin(targetUsername);

        securityLogService.logEvent(adminUsername, "SUPER_KEY_REDEEMED", "SYSTEM", adminSessionId,
                "Admin '" + adminUsername + "' unlocked account '" + targetUsername + "' via Super Key.", "HIGH");

        return true;
    }

    /**
     * Computes the SHA-256 hash of the 4-digit key.
     */
    public static String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
