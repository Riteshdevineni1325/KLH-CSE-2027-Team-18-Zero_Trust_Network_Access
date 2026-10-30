package com.ztna.repository;

import com.ztna.model.SuperRecoveryKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SuperRecoveryKeyRepository extends JpaRepository<SuperRecoveryKey, Long> {

    Optional<SuperRecoveryKey> findTopByTargetUsernameAndAdminSessionIdAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String targetUsername, String adminSessionId, LocalDateTime now);

    /** Used when the key was generated without a target (target supplied only at redeem time). */
    Optional<SuperRecoveryKey> findTopByAdminSessionIdAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String adminSessionId, LocalDateTime now);

    List<SuperRecoveryKey> findByTargetUsernameOrderByCreatedAtDesc(String targetUsername);

    List<SuperRecoveryKey> findByAdminUsernameOrderByCreatedAtDesc(String adminUsername);
}
