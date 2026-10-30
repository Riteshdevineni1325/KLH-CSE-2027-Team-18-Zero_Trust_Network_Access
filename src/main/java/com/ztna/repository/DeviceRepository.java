package com.ztna.repository;

import com.ztna.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByUser_Username(String username);

    List<Device> findByUser_UsernameAndStatus(String username, String status);

    Optional<Device> findByUser_UsernameAndDeviceFingerprint(String username, String fingerprint);

    List<Device> findByStatus(String status);

    boolean existsByUser_UsernameAndDeviceFingerprint(String username, String fingerprint);
}
