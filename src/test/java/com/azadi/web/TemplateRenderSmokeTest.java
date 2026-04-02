package com.azadi.web;

import com.azadi.agreement.AgreementService;
import com.azadi.auth.AuthorizationService;
import com.azadi.auth.Customer;
import com.azadi.bank.BankDetailsService;
import com.azadi.contact.ContactService;
import com.azadi.document.DocumentService;
import com.azadi.payment.PaymentDateService;
import com.azadi.payment.PaymentService;
import com.azadi.payment.PaymentWebhookHandler;
import com.azadi.payment.StripePaymentService;
import com.azadi.settlement.SettlementService;
import com.azadi.statement.StatementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the web layer with Thymeleaf and renders every authenticated page.
 * Catches template errors, missing model attributes, and SpEL failures.
 */
@WebMvcTest
@Import(TestSecurityConfig.class)
class TemplateRenderSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private DocumentService documentService;
    @MockitoBean private AgreementService agreementService;
    @MockitoBean private AuthorizationService authorizationService;
    @MockitoBean private ContactService contactService;
    @MockitoBean private SettlementService settlementService;
    @MockitoBean private StatementService statementService;
    @MockitoBean private BankDetailsService bankDetailsService;
    @MockitoBean private PaymentService paymentService;
    @MockitoBean private PaymentDateService paymentDateService;
    @MockitoBean private StripePaymentService stripePaymentService;
    @MockitoBean private PaymentWebhookHandler webhookHandler;

    @BeforeEach
    void setUpMocks() {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");
        when(authorizationService.getCurrentCustomerName()).thenReturn("Test Customer");
        when(agreementService.getAgreementsForCustomer(any())).thenReturn(List.of());
        when(documentService.getDocumentsForCustomer(any())).thenReturn(List.of());
        when(settlementService.getSettlementsForCustomer(any())).thenReturn(List.of());
        when(statementService.getStatementsForCustomer(any())).thenReturn(List.of());
        when(bankDetailsService.getBankDetails(any())).thenReturn(Optional.empty());

        var customer = new Customer();
        customer.setEmail("test@example.com");
        customer.setPhone("07000000000");
        customer.setAddressLine1("1 Test Street");
        customer.setCity("London");
        customer.setPostcode("SW1A 1AA");
        when(contactService.getCustomer(any())).thenReturn(customer);
    }

    @ParameterizedTest(name = "GET {0} renders without error")
    @ValueSource(strings = {
        "/my-account",
        "/my-documents",
        "/my-contact-details",
        "/finance/make-a-payment",
        "/finance/change-payment-date",
        "/finance/settlement-figure",
        "/finance/request-a-statement",
        "/finance/update-bank-details",
        "/help/faqs",
        "/help/ways-to-pay",
        "/help/contact-us"
    })
    @WithMockUser
    @DisplayName("Every sidebar page renders HTTP 200")
    void everyPage_rendersWithout500(String path) throws Exception {
        mockMvc.perform(get(path))
            .andExpect(status().isOk());
    }
}
