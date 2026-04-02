package com.azadi.audit;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;

    private final JsonMapper objectMapper = new JsonMapper();

    private AuditService auditService;

    private static final String CUSTOMER_ID = "CUST-100";

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditRepository, objectMapper);
    }

    @Test
    @DisplayName("logEvent creates correct AuditEvent with required fields")
    void logEventCreatesCorrectAuditEvent() {
        // Arrange
        String eventType = "LOGIN_SUCCESS";
        String ipAddress = "192.168.1.1";
        Map<String, String> details = Map.of("action", "User logged in successfully");

        when(auditRepository.save(any(AuditEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        auditService.logEvent(CUSTOMER_ID, eventType, ipAddress, details);

        // Assert - logEvent runs async via CompletableFuture, so use timeout
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditRepository, timeout(2000)).save(captor.capture());

        AuditEvent savedEvent = captor.getValue();
        assertThat(savedEvent.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(savedEvent.getEventType()).isEqualTo(eventType);
        assertThat(savedEvent.getIpAddress()).isEqualTo(ipAddress);
        assertThat(savedEvent.getTimestamp()).isNotNull();
        assertThat(savedEvent.getDetails()).contains("User logged in successfully");
    }

    @Test
    @DisplayName("logEvent serializes details map to JSON")
    void logEventSerializesDetailsToJson() {
        // Arrange
        String eventType = "PAGE_VIEW";
        String ipAddress = "10.0.0.1";
        Map<String, String> details = Map.of(
                "page", "agreement-details",
                "agreementId", "AGR-001"
        );

        when(auditRepository.save(any(AuditEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        auditService.logEvent(CUSTOMER_ID, eventType, ipAddress, details);

        // Assert
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditRepository, timeout(2000)).save(captor.capture());

        AuditEvent savedEvent = captor.getValue();
        assertThat(savedEvent.getDetails()).contains("agreement-details");
        assertThat(savedEvent.getDetails()).contains("AGR-001");
    }
}
