package com.ztna.repository;

import com.ztna.model.NetworkAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NetworkAddressRepository extends JpaRepository<NetworkAddress, Long> {

    List<NetworkAddress> findByUser_Username(String username);

    Optional<NetworkAddress> findByUser_UsernameAndIpAddress(String username, String ipAddress);

    List<NetworkAddress> findByUser_UsernameAndStatus(String username, String status);

    List<NetworkAddress> findByUser_UsernameAndZone(String username, String zone);

    List<NetworkAddress> findByStatus(String status);

    boolean existsByUser_UsernameAndIpAddress(String username, String ipAddress);
}
