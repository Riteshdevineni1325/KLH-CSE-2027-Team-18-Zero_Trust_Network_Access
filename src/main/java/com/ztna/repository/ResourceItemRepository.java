package com.ztna.repository;

import com.ztna.model.ResourceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResourceItemRepository extends JpaRepository<ResourceItem, Long> {

    Optional<ResourceItem> findByResourceName(String resourceName);

    boolean existsByResourceName(String resourceName);
}
