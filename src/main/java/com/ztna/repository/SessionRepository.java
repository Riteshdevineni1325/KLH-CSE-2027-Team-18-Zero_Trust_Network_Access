package com.ztna.repository;

import com.ztna.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findBySessionId(String sessionId);

    Optional<Session> findBySessionIdAndActiveTrue(String sessionId);

    List<Session> findByUser_UsernameAndActiveTrue(String username);

    List<Session> findByUser_Username(String username);

    List<Session> findByActiveTrue();
}
