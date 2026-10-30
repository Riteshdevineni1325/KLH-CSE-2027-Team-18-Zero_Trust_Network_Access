package com.ztna.repository;

import com.ztna.model.SecurityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityLogRepository extends JpaRepository<SecurityLog, Long> {

    List<SecurityLog> findByUsernameOrderByEventTimeDesc(String username);

    List<SecurityLog> findByUsername(String username);

    List<SecurityLog> findByEventType(String eventType);

    List<SecurityLog> findBySeverity(String severity);

    List<SecurityLog> findBySessionId(String sessionId);

    List<SecurityLog> findAllByOrderByEventTimeDesc();
}
