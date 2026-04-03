package com.azadi.web;

import com.azadi.agreement.Agreement;
import com.azadi.agreement.AgreementService;
import com.azadi.auth.AuthorizationService;
import com.azadi.payment.PaymentDateController;
import com.azadi.payment.PaymentDateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentDateController.class)
@Import(TestSecurityConfig.class)
class PaymentDateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AgreementService agreementService;
    @MockitoBean private AuthorizationService authorizationService;
    @MockitoBean private PaymentDateService paymentDateService;

    @Test
    @WithMockUser
    @DisplayName("GET /finance/change-payment-date populates model with current payment day")
    void changePaymentDatePage_populatesModel() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");

        var agreement = buildAgreement(1L, false);
        when(agreementService.getAgreementsForCustomer("CUST-1"))
            .thenReturn(List.of(agreement));
        when(paymentDateService.getCurrentPaymentDay(agreement)).thenReturn(15);

        mockMvc.perform(get("/finance/change-payment-date"))
            .andExpect(status().isOk())
            .andExpect(view().name("finance/change-payment-date"))
            .andExpect(model().attributeExists("agreements"))
            .andExpect(model().attribute("currentPaymentDay", 15))
            .andExpect(model().attribute("currentPaymentDate", "15th"))
            .andExpect(model().attribute("alreadyChanged", false));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /finance/change-payment-date with already changed agreement shows flag")
    void changePaymentDatePage_alreadyChanged() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");

        var agreement = buildAgreement(1L, true);
        when(agreementService.getAgreementsForCustomer("CUST-1"))
            .thenReturn(List.of(agreement));
        when(paymentDateService.getCurrentPaymentDay(agreement)).thenReturn(1);

        mockMvc.perform(get("/finance/change-payment-date"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("alreadyChanged", true));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /finance/change-payment-date with valid data redirects with success")
    void changePaymentDate_success() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");

        mockMvc.perform(post("/finance/change-payment-date")
                .with(csrf())
                .param("newPaymentDate", "15")
                .param("agreementId", "1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/finance/change-payment-date"))
            .andExpect(flash().attribute("success", true));

        verify(paymentDateService).changePaymentDate(eq("CUST-1"), eq(1L), eq(15), any());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /finance/change-payment-date with illegal state redirects with error flash")
    void changePaymentDate_alreadyChanged_redirectsWithError() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");
        doThrow(new IllegalStateException("Payment date has already been changed for this agreement."))
            .when(paymentDateService).changePaymentDate(any(), any(), anyInt(), any());

        mockMvc.perform(post("/finance/change-payment-date")
                .with(csrf())
                .param("newPaymentDate", "20")
                .param("agreementId", "1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/finance/change-payment-date"))
            .andExpect(flash().attribute("error", "Payment date has already been changed for this agreement."));
    }

    private Agreement buildAgreement(Long id, boolean paymentDateChanged) {
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
        agreement.setNextPaymentDate(LocalDate.of(2026, 4, 15));
        agreement.setPaymentsRemaining(24);
        agreement.setFinalPaymentDate(LocalDate.of(2028, 3, 1));
        agreement.setPaymentDateChanged(paymentDateChanged);
        return agreement;
    }
}
