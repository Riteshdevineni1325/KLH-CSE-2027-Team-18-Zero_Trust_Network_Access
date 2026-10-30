package com.ztna;

import com.ztna.model.SuperRecoveryKey;
import com.ztna.model.User;
import com.ztna.repository.SuperRecoveryKeyRepository;
import com.ztna.repository.UserRepository;
import com.ztna.service.LoginAttemptService;
import com.ztna.service.SecurityLogService;
import com.ztna.service.SuperKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SuperKeyServiceTest {

    private SuperRecoveryKeyRepository superRecoveryKeyRepository;
    private LoginAttemptService loginAttemptService;
    private UserRepository userRepository;
    private SecurityLogService securityLogService;
    private SuperKeyService superKeyService;

    @BeforeEach
    void setUp() {
        superRecoveryKeyRepository = Mockito.mock(SuperRecoveryKeyRepository.class);
        loginAttemptService = Mockito.mock(LoginAttemptService.class);
        userRepository = Mockito.mock(UserRepository.class);
        securityLogService = Mockito.mock(SecurityLogService.class);
        superKeyService = new SuperKeyService(superRecoveryKeyRepository, loginAttemptService, userRepository, securityLogService);
    }

    @Test
    @DisplayName("Feature 3: Super Key is 4 digits, single-use, and stored ONLY as SHA-256 hash")
    void testSuperKeyGenerationStoresOnlyHash() {
        User eve = new User("eve", "hash", "USER");
        when(userRepository.findByUsername("eve")).thenReturn(Optional.of(eve));

        String plaintextKey = superKeyService.generateSuperKey("admin-session-123", "superadmin", "eve");

        // Verify 4 digits
        assertNotNull(plaintextKey);
        assertEquals(4, plaintextKey.length());
        assertTrue(plaintextKey.matches("\\d{4}"));

        // Capture saved entity in repository
        ArgumentCaptor<SuperRecoveryKey> captor = ArgumentCaptor.forClass(SuperRecoveryKey.class);
        verify(superRecoveryKeyRepository).save(captor.capture());

        SuperRecoveryKey savedKey = captor.getValue();
        assertEquals("admin-session-123", savedKey.getAdminSessionId());
        assertEquals("eve", savedKey.getTargetUsername());
        assertEquals("superadmin", savedKey.getAdminUsername());
        assertFalse(savedKey.isUsed());

        // Plaintext must NEVER equal the stored hash
        assertNotEquals(plaintextKey, savedKey.getHashedKey());
        assertEquals(SuperKeyService.hashKey(plaintextKey), savedKey.getHashedKey());
    }

    @Test
    @DisplayName("Feature 3: Valid Super Key redemption unlocks account and resets attempts")
    void testSuperKeySuccessfulRedemption() {
        String testKey = "4321";
        String hashed = SuperKeyService.hashKey(testKey);

        SuperRecoveryKey recoveryKey = new SuperRecoveryKey(
                "session-1", "superadmin", "eve", hashed, LocalDateTime.now().plusMinutes(10));

        when(superRecoveryKeyRepository
                .findTopByTargetUsernameAndAdminSessionIdAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        eq("eve"), eq("session-1"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(recoveryKey));

        boolean success = superKeyService.redeemSuperKeyAndUnlock("session-1", "superadmin", "eve", testKey);

        assertTrue(success);
        assertTrue(recoveryKey.isUsed());
        assertNotNull(recoveryKey.getUsedAt());

        // Verifies attempts reset by admin
        verify(loginAttemptService).resetAttemptsByAdmin("eve");
    }

    @Test
    @DisplayName("Feature 3: Incorrect Super Key candidate is rejected")
    void testSuperKeyInvalidCandidateRejected() {
        String testKey = "4321";
        String hashed = SuperKeyService.hashKey(testKey);

        SuperRecoveryKey recoveryKey = new SuperRecoveryKey(
                "session-1", "superadmin", "eve", hashed, LocalDateTime.now().plusMinutes(10));

        when(superRecoveryKeyRepository
                .findTopByTargetUsernameAndAdminSessionIdAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        eq("eve"), eq("session-1"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(recoveryKey));

        boolean success = superKeyService.redeemSuperKeyAndUnlock("session-1", "superadmin", "eve", "0000");

        assertFalse(success);
        assertFalse(recoveryKey.isUsed());
        verify(loginAttemptService, never()).resetAttemptsByAdmin(any());
    }
}
