package com.azadi.web;

import com.azadi.agreement.Agreement;
import com.azadi.agreement.AgreementService;
import com.azadi.auth.AuthorizationService;
import com.azadi.auth.Customer;
import com.azadi.contact.ContactService;
import com.azadi.statement.StatementController;
import com.azadi.statement.StatementRequest;
import com.azadi.statement.StatementService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatementController.class)
@Import(TestSecurityConfig.class)
class StatementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private StatementService statementService;
    @MockitoBean private AgreementService agreementService;
    @MockitoBean private AuthorizationService authorizationService;
    @MockitoBean private ContactService contactService;

    @Test
    @WithMockUser
    @DisplayName("GET /finance/request-a-statement populates model with agreements, statements, email, address")
    void statementPage_populatesFullModel() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");

        var agreement = buildAgreement(1L);
        when(agreementService.getAgreementsForCustomer("CUST-1"))
            .thenReturn(List.of(agreement));

        var statement = buildStatementRequest(1L, 1L);
        when(statementService.getStatementsForCustomer("CUST-1"))
            .thenReturn(List.of(statement));

        when(contactService.getCustomer("CUST-1")).thenReturn(buildCustomer());

        mockMvc.perform(get("/finance/request-a-statement"))
            .andExpect(status().isOk())
            .andExpect(view().name("finance/request-a-statement"))
            .andExpect(model().attributeExists("agreements"))
            .andExpect(model().attributeExists("statements"))
            .andExpect(model().attribute("email", "test@example.com"))
            .andExpect(model().attribute("address", "1 Test Street"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /finance/request-a-statement with agreementId redirects with success")
    void requestStatement_withAgreementId_redirectsWithSuccess() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");
        when(agreementService.getAgreementsForCustomer("CUST-1"))
            .thenReturn(List.of(buildAgreement(1L)));

        mockMvc.perform(post("/finance/request-a-statement")
                .with(csrf())
                .param("agreementId", "1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/finance/request-a-statement"))
            .andExpect(flash().attribute("success",
                "Statement requested successfully. You will receive it by email."));

        verify(statementService).requestStatement(eq("CUST-1"), eq(1L), any());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /finance/request-a-statement without agreementId falls back to first agreement")
    void requestStatement_noAgreementId_fallsBackToFirst() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");

        var agreement = buildAgreement(42L);
        when(agreementService.getAgreementsForCustomer("CUST-1"))
            .thenReturn(List.of(agreement));

        mockMvc.perform(post("/finance/request-a-statement")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/finance/request-a-statement"));

        verify(statementService).requestStatement(eq("CUST-1"), eq(42L), any());
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

    private StatementRequest buildStatementRequest(Long id, Long agreementId) {
        var request = new StatementRequest();
        request.setId(id);
        request.setCustomerId("CUST-1");
        request.setAgreementId(agreementId);
        request.setStatus(StatementRequest.STATUS_PENDING);
        request.setRequestedAt(Instant.now());
        return request;
    }

    private Customer buildCustomer() {
        var customer = new Customer();
        customer.setCustomerId("CUST-1");
        customer.setFullName("Test Customer");
        customer.setEmail("test@example.com");
        customer.setPhone("07000000000");
        customer.setAddressLine1("1 Test Street");
        customer.setCity("London");
        customer.setPostcode("SW1A 1AA");
        return customer;
    }
}
