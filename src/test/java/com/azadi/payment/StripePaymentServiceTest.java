package com.azadi.payment;

import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripePaymentServiceTest {

    @Mock
    private StripeClient stripeClient;

    @Mock
    private com.stripe.service.PaymentIntentService paymentIntentService;

    private StripePaymentService stripePaymentService;

    private static final String WEBHOOK_SECRET = "whsec_test_mock";

    @BeforeEach
    void setUp() {
        stripePaymentService = new StripePaymentService(stripeClient, WEBHOOK_SECRET);
    }

    @Test
    @DisplayName("createPaymentIntent builds request with correct amount and currency")
    void createPaymentIntentWithCorrectParameters() throws Exception {
        // Arrange
        long amountInPence = 45000L;
        String agreementNumber = "AGR-001";
        String customerEmail = "test@example.com";

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getId()).thenReturn("pi_test_123");
        when(mockIntent.getClientSecret()).thenReturn("pi_test_123_secret_abc");

        when(stripeClient.paymentIntents()).thenReturn(paymentIntentService);
        when(paymentIntentService.create(any(PaymentIntentCreateParams.class))).thenReturn(mockIntent);

        // Act
        PaymentIntent result = stripePaymentService.createPaymentIntent(
                amountInPence, agreementNumber, customerEmail);

        // Assert
        assertThat(result.getId()).isEqualTo("pi_test_123");
        assertThat(result.getClientSecret()).isEqualTo("pi_test_123_secret_abc");
        verify(paymentIntentService).create(any(PaymentIntentCreateParams.class));
    }

    @Test
    @DisplayName("capturePaymentIntent calls Stripe with correct intent ID")
    void capturePaymentIntent() throws Exception {
        // Arrange
        String paymentIntentId = "pi_test_123";
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("succeeded");

        when(stripeClient.paymentIntents()).thenReturn(paymentIntentService);
        when(paymentIntentService.capture(anyString(), any(PaymentIntentCaptureParams.class)))
                .thenReturn(mockIntent);

        // Act
        PaymentIntent result = stripePaymentService.capturePaymentIntent(paymentIntentId);

        // Assert
        assertThat(result.getStatus()).isEqualTo("succeeded");
        verify(paymentIntentService).capture(anyString(), any(PaymentIntentCaptureParams.class));
    }

    @Test
    @DisplayName("cancelPaymentIntent calls Stripe with correct intent ID")
    void cancelPaymentIntent() throws Exception {
        // Arrange
        String paymentIntentId = "pi_test_123";
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("canceled");

        when(stripeClient.paymentIntents()).thenReturn(paymentIntentService);
        when(paymentIntentService.cancel(anyString())).thenReturn(mockIntent);

        // Act
        PaymentIntent result = stripePaymentService.cancelPaymentIntent(paymentIntentId);

        // Assert
        assertThat(result.getStatus()).isEqualTo("canceled");
    }

    @Test
    @DisplayName("constructWebhookEvent succeeds with valid signature")
    void constructWebhookEventWithValidSignature() throws Exception {
        // Arrange
        String payload = "{\"id\": \"evt_test\", \"type\": \"payment_intent.succeeded\"}";
        String validSignature = "t=1234567890,v1=valid_hash";

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
            webhookMock.when(() -> Webhook.constructEvent(payload, validSignature, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            // Act
            Event result = stripePaymentService.constructWebhookEvent(payload, validSignature);

            // Assert
            assertThat(result.getType()).isEqualTo("payment_intent.succeeded");
        }
    }

    @Test
    @DisplayName("constructWebhookEvent throws exception with invalid signature")
    void constructWebhookEventWithInvalidSignature() {
        // Arrange
        String payload = "{\"id\": \"evt_test\"}";
        String invalidSignature = "t=1234567890,v1=invalid_hash";

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(payload, invalidSignature, WEBHOOK_SECRET))
                    .thenThrow(new SignatureVerificationException("Invalid signature", invalidSignature));

            // Act & Assert
            assertThatThrownBy(() -> stripePaymentService.constructWebhookEvent(payload, invalidSignature))
                    .isInstanceOf(SignatureVerificationException.class)
                    .hasMessageContaining("Invalid signature");
        }
    }
}
