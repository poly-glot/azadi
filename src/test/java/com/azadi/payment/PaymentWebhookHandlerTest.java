package com.azadi.payment;

import com.azadi.audit.AuditService;
import com.azadi.email.EmailService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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
class PaymentWebhookHandlerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditService auditService;

    @Mock
    private StripePaymentService stripePaymentService;

    private PaymentWebhookHandler webhookHandler;

    private static final String CUSTOMER_ID = "CUST-001";
    private static final long AMOUNT_PENCE = 112732L;

    @BeforeEach
    void setUp() {
        webhookHandler = new PaymentWebhookHandler(paymentRepository, emailService, auditService, stripePaymentService);
    }

    @Test
    @DisplayName("handleEvent with payment_intent.succeeded updates record and sends email")
    void handlePaymentSuccessUpdatesAndEmails() throws SignatureVerificationException {
        // Arrange
        var record = new PaymentRecord();
        record.setCustomerId(CUSTOMER_ID);
        record.setAmountPence(AMOUNT_PENCE);
        record.setStatus(PaymentRecord.STATUS_PENDING);

        var paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getId()).thenReturn("pi_test_123");

        var deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(paymentIntent));

        var event = mock(Event.class);
        when(event.getType()).thenReturn("payment_intent.succeeded");
        when(event.getId()).thenReturn("evt_123");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        when(stripePaymentService.constructWebhookEvent("payload", "sig")).thenReturn(event);
        when(paymentRepository.findByWebhookEventId("evt_123")).thenReturn(Optional.empty());
        when(paymentRepository.findByStripePaymentIntentId("pi_test_123")).thenReturn(Optional.of(record));
        when(paymentRepository.save(any(PaymentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        var response = webhookHandler.handleEvent("payload", "sig", "127.0.0.1");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(record.getStatus()).isEqualTo(PaymentRecord.STATUS_COMPLETED);
        verify(emailService).sendPaymentConfirmation(CUSTOMER_ID, AMOUNT_PENCE);
        verify(auditService).logEvent(eq(CUSTOMER_ID), eq("PAYMENT_COMPLETED"), anyString(), any());
    }

    @Test
    @DisplayName("handleEvent with payment_intent.payment_failed updates record status to FAILED")
    void handlePaymentFailureUpdatesStatus() throws SignatureVerificationException {
        // Arrange
        var record = new PaymentRecord();
        record.setCustomerId(CUSTOMER_ID);
        record.setStatus(PaymentRecord.STATUS_PENDING);

        var paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getId()).thenReturn("pi_fail");

        var deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(paymentIntent));

        var event = mock(Event.class);
        when(event.getType()).thenReturn("payment_intent.payment_failed");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        when(stripePaymentService.constructWebhookEvent("payload", "sig")).thenReturn(event);
        when(paymentRepository.findByStripePaymentIntentId("pi_fail")).thenReturn(Optional.of(record));
        when(paymentRepository.save(any(PaymentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        var response = webhookHandler.handleEvent("payload", "sig", "127.0.0.1");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(record.getStatus()).isEqualTo(PaymentRecord.STATUS_FAILED);
        verify(emailService, never()).sendPaymentConfirmation(anyString(), anyLong());
    }

    @Test
    @DisplayName("Duplicate webhook event is ignored (idempotency)")
    void duplicateWebhookEventIgnored() throws SignatureVerificationException {
        // Arrange
        var existingRecord = new PaymentRecord();

        var paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getId()).thenReturn("pi_dup");

        var deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(paymentIntent));

        var event = mock(Event.class);
        when(event.getType()).thenReturn("payment_intent.succeeded");
        when(event.getId()).thenReturn("evt_dup");
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        when(stripePaymentService.constructWebhookEvent("payload", "sig")).thenReturn(event);
        when(paymentRepository.findByWebhookEventId("evt_dup")).thenReturn(Optional.of(existingRecord));

        // Act
        var response = webhookHandler.handleEvent("payload", "sig", "127.0.0.1");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentRepository, never()).findByStripePaymentIntentId(anyString());
        verify(emailService, never()).sendPaymentConfirmation(anyString(), anyLong());
    }

    @Test
    @DisplayName("Invalid webhook signature returns 400")
    void invalidSignatureReturnsBadRequest() throws SignatureVerificationException {
        when(stripePaymentService.constructWebhookEvent("payload", "bad_sig"))
            .thenThrow(new SignatureVerificationException("Invalid signature", "bad_sig"));

        var response = webhookHandler.handleEvent("payload", "bad_sig", "127.0.0.1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
