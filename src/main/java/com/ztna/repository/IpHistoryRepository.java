package com.ztna.repository;

import com.ztna.model.IpHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IpHistoryRepository extends JpaRepository<IpHistory, Long> {

    List<IpHistory> findByUser_UsernameOrderBySeenAtDesc(String username);

    List<IpHistory> findByUser_UsernameAndIpAddress(String username, String ipAddress);

    List<IpHistory> findByUser_UsernameAndEventType(String username, String eventType);
}
