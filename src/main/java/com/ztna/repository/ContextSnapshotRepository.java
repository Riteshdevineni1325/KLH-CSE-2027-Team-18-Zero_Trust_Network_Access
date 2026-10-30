package com.ztna.repository;

import com.ztna.model.ContextSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContextSnapshotRepository extends JpaRepository<ContextSnapshot, Long> {

    List<ContextSnapshot> findByUsernameOrderByCapturedAtDesc(String username);

    Optional<ContextSnapshot> findTopByUsernameOrderByCapturedAtDesc(String username);

    List<ContextSnapshot> findBySessionId(String sessionId);

    Optional<ContextSnapshot> findTopBySessionIdOrderByCapturedAtDesc(String sessionId);
}
