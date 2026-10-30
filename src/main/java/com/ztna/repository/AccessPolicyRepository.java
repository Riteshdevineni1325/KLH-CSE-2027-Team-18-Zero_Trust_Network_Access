package com.ztna.repository;

import com.ztna.model.AccessPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessPolicyRepository extends JpaRepository<AccessPolicy, Long> {

    Optional<AccessPolicy> findByResourceName(String resourceName);

    Optional<AccessPolicy> findByPolicyName(String policyName);

    boolean existsByPolicyName(String policyName);

    boolean existsByResourceNameAndRequiredRoleAndAllowedNetworkAndRequiredDeviceStatusAndEffect(
            String resourceName, String requiredRole, String allowedNetwork, String requiredDeviceStatus, String effect);

    List<AccessPolicy> findByResourceNameAndEnabledTrue(String resourceName);

    List<AccessPolicy> findByResourceNameAndEnabledTrueOrderByPriorityAsc(String resourceName);

    List<AccessPolicy> findByEnabledTrue();

    List<AccessPolicy> findByRequiredRole(String requiredRole);
}
