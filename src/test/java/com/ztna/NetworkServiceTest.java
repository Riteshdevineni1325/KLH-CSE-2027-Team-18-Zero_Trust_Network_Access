package com.ztna;

import com.ztna.model.NetworkRule;
import com.ztna.repository.NetworkAddressRepository;
import com.ztna.repository.NetworkRuleRepository;
import com.ztna.repository.UserRepository;
import com.ztna.service.NetworkService;
import com.ztna.service.SecurityLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.ztna.repository.IpHistoryRepository;

class NetworkServiceTest {

    private NetworkRuleRepository networkRuleRepository;
    private NetworkAddressRepository networkAddressRepository;
    private IpHistoryRepository ipHistoryRepository;
    private UserRepository userRepository;
    private SecurityLogService securityLogService;
    private NetworkService networkService;

    @BeforeEach
    void setUp() {
        networkRuleRepository    = Mockito.mock(NetworkRuleRepository.class);
        networkAddressRepository = Mockito.mock(NetworkAddressRepository.class);
        ipHistoryRepository      = Mockito.mock(IpHistoryRepository.class);
        userRepository           = Mockito.mock(UserRepository.class);
        securityLogService       = Mockito.mock(SecurityLogService.class);
        networkService = new NetworkService(
                networkRuleRepository, networkAddressRepository,
                ipHistoryRepository, userRepository, securityLogService);
    }

    @Test
    @DisplayName("Dual IP CIDR Matcher: Matches exact IP and CIDR subnets correctly")
    void testCidrMatchingLogic() {
        assertTrue(NetworkService.matchesIpOrCidr("192.168.1.50", "192.168.1.0/24"));
        assertTrue(NetworkService.matchesIpOrCidr("10.0.5.12", "10.0.0.0/16"));
        assertTrue(NetworkService.matchesIpOrCidr("127.0.0.1", "127.0.0.1"));

        assertFalse(NetworkService.matchesIpOrCidr("192.168.2.1", "192.168.1.0/24"));
        assertFalse(NetworkService.matchesIpOrCidr("172.16.0.1", "10.0.0.0/16"));
    }

    @Test
    @DisplayName("Network Classification: Priority is BLOCKED > OFFICE > HOME > UNKNOWN")
    void testClassificationPriority() {
        NetworkRule officeRule = new NetworkRule("192.168.1.0/24", "OFFICE", "HQ Office");
        NetworkRule blockedRule = new NetworkRule("192.168.1.100", "BLOCKED", "Compromised Node");

        when(networkRuleRepository.findByActiveTrue()).thenReturn(List.of(officeRule, blockedRule));

        // IP in office subnet but explicitly blocked should classify as BLOCKED
        assertEquals("BLOCKED", networkService.classifyIp("192.168.1.100"));

        // Regular IP in office subnet classifies as OFFICE
        assertEquals("OFFICE", networkService.classifyIp("192.168.1.50"));

        // Unmatched external IP classifies as UNKNOWN
        assertEquals("UNKNOWN", networkService.classifyIp("8.8.8.8"));
    }
}
