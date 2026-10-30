package com.ztna.repository;

import com.ztna.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRole(String role);

    List<User> findByStatus(String status);

    List<User> findByLockedTrue();
}
