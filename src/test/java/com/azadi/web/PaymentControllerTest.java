package com.azadi.web;

import com.azadi.agreement.Agreement;
import com.azadi.agreement.AgreementService;
import com.azadi.auth.AuthorizationService;
import com.azadi.payment.PaymentController;
import com.azadi.payment.PaymentService;
import com.azadi.payment.PaymentWebhookHandler;
import com.azadi.payment.StripePaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(TestSecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private PaymentService paymentService;
    @MockitoBean private PaymentWebhookHandler webhookHandler;
    @MockitoBean private StripePaymentService stripePaymentService;
    @MockitoBean private AgreementService agreementService;
    @MockitoBean private AuthorizationService authorizationService;

    @Test
    @WithMockUser
    @DisplayName("GET /finance/make-a-payment populates agreements and stripePublishableKey")
    void makePaymentPage_populatesModel() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");

        var agreement = buildAgreement(1L);
        when(agreementService.getAgreementsForCustomer("CUST-1"))
            .thenReturn(List.of(agreement));

        mockMvc.perform(get("/finance/make-a-payment"))
            .andExpect(status().isOk())
            .andExpect(view().name("finance/make-a-payment"))
            .andExpect(model().attributeExists("agreements"))
            .andExpect(model().attribute("agreements", hasSize(1)))
            .andExpect(model().attributeExists("stripePublishableKey"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /finance/make-a-payment returns JSON with clientSecret")
    void makePayment_returnsClientSecret() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");

        var agreement = buildAgreement(1L);
        when(agreementService.getAgreement("CUST-1", 1L)).thenReturn(agreement);

        var paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getClientSecret()).thenReturn("pi_secret_123");
        when(paymentService.initiatePayment(anyLong(), anyLong(), anyString(), anyString(), anyString()))
            .thenReturn(paymentIntent);

        mockMvc.perform(post("/finance/make-a-payment")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amountPence\": 5000, \"agreementId\": 1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clientSecret").value("pi_secret_123"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/stripe/webhook with valid signature returns ok")
    void webhook_validSignature_returnsOk() throws Exception {
        var event = mock(Event.class);
        when(event.getType()).thenReturn("payment_intent.succeeded");
        when(event.getId()).thenReturn("evt_123");

        var paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getId()).thenReturn("pi_123");

        var deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(paymentIntent));
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);

        when(stripePaymentService.constructWebhookEvent(anyString(), anyString()))
            .thenReturn(event);

        mockMvc.perform(post("/api/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\": \"payment_intent.succeeded\"}")
                .header("Stripe-Signature", "valid_sig"))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/stripe/webhook with invalid signature returns 400")
    void webhook_invalidSignature_returnsBadRequest() throws Exception {
        when(stripePaymentService.constructWebhookEvent(anyString(), anyString()))
            .thenThrow(new SignatureVerificationException("Invalid signature", "sig_header"));

        mockMvc.perform(post("/api/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .header("Stripe-Signature", "invalid_sig"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Invalid signature"));
    }

    private Agreement buildAgreement(Long id) {
        var agreement = new Agreement();
        agreement.setId(id);
        agreement.setAgreementNumber("AGR-001");
        agreement.setCustomerId("CUST-1");
        agreement.setType("PCP");
        agreement.setVehicleModel("2024 Test Vehicle");
        agreement.setRegistration("AB24 TST");
        agreement.setBalancePence(1200000L);
        agreement.setApr("6.9");
        agreement.setOriginalTermMonths(48);
        agreement.setContractMileage(10000);
        agreement.setExcessPricePerMilePence(10L);
        agreement.setLastPaymentPence(45000L);
        agreement.setLastPaymentDate(LocalDate.of(2026, 3, 1));
        agreement.setNextPaymentPence(45000L);
        agreement.setNextPaymentDate(LocalDate.of(2026, 4, 1));
        agreement.setPaymentsRemaining(24);
        agreement.setFinalPaymentDate(LocalDate.of(2028, 3, 1));
        return agreement;
    }
}
