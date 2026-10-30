package com.ztna.repository;

import com.ztna.model.AdminPromotionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AdminPromotionRequestRepository extends JpaRepository<AdminPromotionRequest, Long> {

    List<AdminPromotionRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<AdminPromotionRequest> findAllByOrderByCreatedAtDesc();

    List<AdminPromotionRequest> findByRequesterUsernameOrderByCreatedAtDesc(String requesterUsername);

    Optional<AdminPromotionRequest> findFirstByRequesterUsernameAndStatus(String requesterUsername, String status);

    boolean existsByRequesterUsernameAndStatus(String requesterUsername, String status);

    void deleteByRequesterUsername(String requesterUsername);
}
