package com.azadi.payment;

import com.azadi.audit.AuditService;
import com.azadi.auth.AuthorizationService;
import com.azadi.email.EmailService;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private StripePaymentService stripePaymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditService auditService;

    @Mock
    private AuthorizationService authorizationService;

    private PaymentService paymentService;

    private static final String CUSTOMER_ID = "CUST-001";
    private static final String AGREEMENT_NUMBER = "AGR-100001";
    private static final long AMOUNT_PENCE = 112732L;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
            stripePaymentService, paymentRepository, emailService,
            auditService, authorizationService);
    }

    @Test
    @DisplayName("initiatePayment creates Stripe intent and saves payment record")
    void initiatePaymentCreatesIntentAndSaves() throws Exception {
        // Arrange
        when(authorizationService.getCurrentCustomerId()).thenReturn(CUSTOMER_ID);
        var mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getId()).thenReturn("pi_test_123");
        when(stripePaymentService.createPaymentIntent(AMOUNT_PENCE, AGREEMENT_NUMBER, "test@test.com"))
            .thenReturn(mockIntent);
        when(paymentRepository.save(any(PaymentRecord.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // Act
        var result = paymentService.initiatePayment(
            AMOUNT_PENCE, 1L, AGREEMENT_NUMBER, "test@test.com", "127.0.0.1");

        // Assert
        assertThat(result.getId()).isEqualTo("pi_test_123");
        verify(paymentRepository).save(any(PaymentRecord.class));
        verify(auditService).logEvent(eq(CUSTOMER_ID), eq("PAYMENT_INITIATED"), eq("127.0.0.1"), any());
    }

    @Test
    @DisplayName("handlePaymentSuccess updates record and sends email")
    void handlePaymentSuccessUpdatesAndEmails() {
        // Arrange
        var record = new PaymentRecord();
        record.setCustomerId(CUSTOMER_ID);
        record.setAmountPence(AMOUNT_PENCE);
        record.setStatus(PaymentRecord.STATUS_PENDING);

        when(paymentRepository.findByWebhookEventId("evt_123")).thenReturn(Optional.empty());
        when(paymentRepository.findByStripePaymentIntentId("pi_test_123")).thenReturn(Optional.of(record));
        when(paymentRepository.save(any(PaymentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        paymentService.handlePaymentSuccess("pi_test_123", "evt_123", "127.0.0.1");

        // Assert
        assertThat(record.getStatus()).isEqualTo(PaymentRecord.STATUS_COMPLETED);
        verify(emailService).sendPaymentConfirmation(CUSTOMER_ID, AMOUNT_PENCE);
        verify(auditService).logEvent(eq(CUSTOMER_ID), eq("PAYMENT_COMPLETED"), anyString(), any());
    }

    @Test
    @DisplayName("handlePaymentFailure updates record status to FAILED")
    void handlePaymentFailureUpdatesStatus() {
        // Arrange
        var record = new PaymentRecord();
        record.setCustomerId(CUSTOMER_ID);
        record.setStatus(PaymentRecord.STATUS_PENDING);

        when(paymentRepository.findByStripePaymentIntentId("pi_fail")).thenReturn(Optional.of(record));
        when(paymentRepository.save(any(PaymentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        paymentService.handlePaymentFailure("pi_fail", "evt_fail", "127.0.0.1");

        // Assert
        assertThat(record.getStatus()).isEqualTo(PaymentRecord.STATUS_FAILED);
        verify(emailService, never()).sendPaymentConfirmation(anyString(), anyLong());
    }

    @Test
    @DisplayName("Duplicate webhook event is ignored (idempotency)")
    void duplicateWebhookEventIgnored() {
        // Arrange
        var existingRecord = new PaymentRecord();
        when(paymentRepository.findByWebhookEventId("evt_dup")).thenReturn(Optional.of(existingRecord));

        // Act
        paymentService.handlePaymentSuccess("pi_dup", "evt_dup", "127.0.0.1");

        // Assert
        verify(paymentRepository, never()).findByStripePaymentIntentId(anyString());
        verify(emailService, never()).sendPaymentConfirmation(anyString(), anyLong());
    }
}
