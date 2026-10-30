package com.ztna.repository;

import com.ztna.model.ContextData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContextDataRepository
        extends JpaRepository<ContextData, Long> {

    List<ContextData> findByUsername(String username);
}