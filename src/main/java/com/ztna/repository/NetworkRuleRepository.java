package com.ztna.repository;

import com.ztna.model.NetworkRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkRuleRepository extends JpaRepository<NetworkRule, Long> {

    @Query("SELECT r FROM NetworkRule r WHERE r.ipOrCidr = :ipOrCidr")
    Optional<NetworkRule> findByIpOrCidr(@Param("ipOrCidr") String ipOrCidr);

    List<NetworkRule> findByActiveTrue();

    List<NetworkRule> findByClassification(String classification);
}
