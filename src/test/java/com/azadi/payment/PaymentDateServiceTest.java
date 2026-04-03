package com.azadi.payment;

import com.azadi.agreement.Agreement;
import com.azadi.agreement.AgreementRepository;
import com.azadi.agreement.AgreementService;
import com.azadi.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentDateServiceTest {

    @Mock private AgreementRepository agreementRepository;
    @Mock private AgreementService agreementService;
    @Mock private AuditService auditService;

    private PaymentDateService service;

    private static final String CUSTOMER_ID = "CUST-100";

    @BeforeEach
    void setUp() {
        service = new PaymentDateService(agreementRepository, agreementService, auditService);
    }

    @Test
    @DisplayName("Changes payment date and marks agreement as changed")
    void changePaymentDate_success() {
        var agreement = buildAgreement(1L, CUSTOMER_ID, false, LocalDate.of(2026, 5, 8));
        when(agreementService.getAgreement(CUSTOMER_ID, 1L)).thenReturn(agreement);

        service.changePaymentDate(CUSTOMER_ID, 1L, 15, "127.0.0.1");

        assertThat(agreement.getNextPaymentDate().getDayOfMonth()).isEqualTo(15);
        assertThat(agreement.isPaymentDateChanged()).isTrue();
        verify(agreementRepository).save(agreement);
        verify(auditService).logEvent(eq(CUSTOMER_ID), eq("PAYMENT_DATE_CHANGED"), eq("127.0.0.1"), any());
    }

    @Test
    @DisplayName("Rejects second date change on same agreement")
    void changePaymentDate_alreadyChanged_throws() {
        var agreement = buildAgreement(1L, CUSTOMER_ID, true, LocalDate.of(2026, 5, 15));
        when(agreementService.getAgreement(CUSTOMER_ID, 1L)).thenReturn(agreement);

        assertThatThrownBy(() -> service.changePaymentDate(CUSTOMER_ID, 1L, 20, "127.0.0.1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already been changed");
    }

    @Test
    @DisplayName("Rejects invalid day outside 1-28")
    void changePaymentDate_invalidDay_throws() {
        var agreement = buildAgreement(1L, CUSTOMER_ID, false, LocalDate.of(2026, 5, 8));
        when(agreementService.getAgreement(CUSTOMER_ID, 1L)).thenReturn(agreement);

        assertThatThrownBy(() -> service.changePaymentDate(CUSTOMER_ID, 1L, 30, "127.0.0.1"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Throws when agreement not found")
    void changePaymentDate_notFound_throws() {
        when(agreementService.getAgreement(CUSTOMER_ID, 99L))
            .thenThrow(new NoSuchElementException("Agreement not found: 99"));

        assertThatThrownBy(() -> service.changePaymentDate(CUSTOMER_ID, 99L, 15, "127.0.0.1"))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("Throws when customer does not own agreement")
    void changePaymentDate_wrongCustomer_throws() {
        when(agreementService.getAgreement(CUSTOMER_ID, 1L))
            .thenThrow(new AccessDeniedException("You do not have access to this agreement."));

        assertThatThrownBy(() -> service.changePaymentDate(CUSTOMER_ID, 1L, 15, "127.0.0.1"))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getCurrentPaymentDay returns day of month from next payment date")
    void getCurrentPaymentDay_returnsDay() {
        var agreement = buildAgreement(1L, CUSTOMER_ID, false, LocalDate.of(2026, 5, 22));
        assertThat(service.getCurrentPaymentDay(agreement)).isEqualTo(22);
    }

    @Test
    @DisplayName("getCurrentPaymentDay returns 1 when no next payment date")
    void getCurrentPaymentDay_nullDate_returns1() {
        var agreement = buildAgreement(1L, CUSTOMER_ID, false, null);
        assertThat(service.getCurrentPaymentDay(agreement)).isEqualTo(1);
    }

    private Agreement buildAgreement(Long id, String customerId, boolean dateChanged, LocalDate nextDate) {
        var a = new Agreement();
        a.setId(id);
        a.setCustomerId(customerId);
        a.setPaymentDateChanged(dateChanged);
        a.setNextPaymentDate(nextDate);
        return a;
    }
}
