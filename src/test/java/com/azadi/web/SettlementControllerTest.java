package com.azadi.web;

import com.azadi.agreement.Agreement;
import com.azadi.agreement.AgreementService;
import com.azadi.auth.AuthorizationService;
import com.azadi.settlement.SettlementController;
import com.azadi.settlement.SettlementFigure;
import com.azadi.settlement.SettlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SettlementController.class)
@Import(TestSecurityConfig.class)
class SettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private SettlementService settlementService;
    @MockitoBean private AgreementService agreementService;
    @MockitoBean private AuthorizationService authorizationService;

    @Test
    @WithMockUser
    @DisplayName("GET /finance/settlement-figure populates agreements and settlements")
    void settlementPage_populatesModel() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");

        var agreement = buildAgreement(1L);
        when(agreementService.getAgreementsForCustomer("CUST-1"))
            .thenReturn(List.of(agreement));

        var figure = buildSettlementFigure(1L, 1L);
        when(settlementService.getSettlementsForCustomer("CUST-1"))
            .thenReturn(List.of(figure));

        mockMvc.perform(get("/finance/settlement-figure"))
            .andExpect(status().isOk())
            .andExpect(view().name("finance/settlement-figure"))
            .andExpect(model().attribute("agreements", hasSize(1)))
            .andExpect(model().attributeExists("settlements"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /finance/settlement-figure with agreementId calculates and renders")
    void calculateSettlement_rendersResult() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");

        var agreement = buildAgreement(1L);
        when(agreementService.getAgreementsForCustomer("CUST-1"))
            .thenReturn(List.of(agreement));

        var figure = buildSettlementFigure(1L, 1L);
        when(settlementService.calculateSettlement("CUST-1", 1L)).thenReturn(figure);

        mockMvc.perform(post("/finance/settlement-figure")
                .with(csrf())
                .param("agreementId", "1"))
            .andExpect(status().isOk())
            .andExpect(view().name("finance/settlement-figure"))
            .andExpect(model().attributeExists("settlement"))
            .andExpect(model().attributeExists("agreements"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /finance/settlement-figure with mobileNumber redirects with smsSent flash")
    void sendSettlementSms_redirectsWithFlash() throws Exception {
        mockMvc.perform(post("/finance/settlement-figure")
                .with(csrf())
                .param("mobileNumber", "07999999999"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/finance/settlement-figure"))
            .andExpect(flash().attribute("smsSent", true));
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

    private SettlementFigure buildSettlementFigure(Long id, Long agreementId) {
        var figure = new SettlementFigure();
        figure.setId(id);
        figure.setAgreementId(agreementId);
        figure.setCustomerId("CUST-1");
        figure.setAmountPence(1224000L);
        figure.setCalculatedAt(Instant.now());
        figure.setValidUntil(LocalDate.of(2026, 4, 30));
        return figure;
    }
}
