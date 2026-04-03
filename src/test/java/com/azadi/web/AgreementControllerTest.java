package com.azadi.web;

import com.azadi.agreement.Agreement;
import com.azadi.agreement.AgreementController;
import com.azadi.agreement.AgreementService;
import com.azadi.auth.AuthorizationService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgreementController.class)
@Import(TestSecurityConfig.class)
class AgreementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private AgreementService agreementService;
    @MockitoBean private AuthorizationService authorizationService;

    @Test
    @WithMockUser
    @DisplayName("GET /my-account populates agreements model attribute")
    void myAccount_populatesAgreements() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");

        var agreement = buildAgreement(1L, "AGR-001");
        when(agreementService.getAgreementsForCustomer("CUST-1"))
            .thenReturn(List.of(agreement));

        mockMvc.perform(get("/my-account"))
            .andExpect(status().isOk())
            .andExpect(view().name("my-account"))
            .andExpect(model().attributeExists("agreements"))
            .andExpect(model().attribute("agreements", hasSize(1)));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /my-account with no agreements renders empty list")
    void myAccount_emptyAgreements() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");
        when(agreementService.getAgreementsForCustomer("CUST-1")).thenReturn(List.of());

        mockMvc.perform(get("/my-account"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("agreements", hasSize(0)));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /agreements/{id} renders detail for owned agreement")
    void agreementDetail_rendersForOwner() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");
        when(authorizationService.isOwner(1L)).thenReturn(true);

        var agreement = buildAgreement(1L, "AGR-001");
        when(agreementService.getAgreement("CUST-1", 1L)).thenReturn(agreement);

        mockMvc.perform(get("/agreements/1"))
            .andExpect(status().isOk())
            .andExpect(view().name("agreement-detail"))
            .andExpect(model().attributeExists("agreement"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /agreements/{id} delegates ownership check to service")
    void agreementDetail_delegatesOwnershipCheck() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");

        var agreement = buildAgreement(5L, "AGR-005");
        when(agreementService.getAgreement("CUST-1", 5L)).thenReturn(agreement);

        mockMvc.perform(get("/agreements/5"))
            .andExpect(status().isOk());

        verify(agreementService).getAgreement("CUST-1", 5L);
    }

    private Agreement buildAgreement(Long id, String agreementNumber) {
        var agreement = new Agreement();
        agreement.setId(id);
        agreement.setAgreementNumber(agreementNumber);
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
