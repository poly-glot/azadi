package com.azadi.web;

import com.azadi.auth.AuthorizationService;
import com.azadi.bank.BankDetails;
import com.azadi.bank.BankDetailsController;
import com.azadi.bank.BankDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankDetailsController.class)
@Import(TestSecurityConfig.class)
class BankDetailsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private BankDetailsService bankDetailsService;
    @MockitoBean private AuthorizationService authorizationService;

    @Test
    @WithMockUser
    @DisplayName("GET /finance/update-bank-details renders form with existing bank details")
    void bankDetailsForm_rendersWithExistingDetails() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");

        var details = new BankDetails();
        details.setAccountHolderName("John Doe");
        details.setLastFourAccount("1234");
        details.setLastTwoSortCode("56");
        when(bankDetailsService.getBankDetails("CUST-1")).thenReturn(Optional.of(details));

        mockMvc.perform(get("/finance/update-bank-details"))
            .andExpect(status().isOk())
            .andExpect(view().name("finance/update-bank-details"))
            .andExpect(model().attributeExists("bankDetails"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /finance/update-bank-details renders empty form when no details exist")
    void bankDetailsForm_rendersEmptyForm() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");
        when(bankDetailsService.getBankDetails("CUST-1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/finance/update-bank-details"))
            .andExpect(status().isOk())
            .andExpect(view().name("finance/update-bank-details"))
            .andExpect(model().attributeDoesNotExist("bankDetails"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST with valid data redirects with success flash")
    void updateBankDetails_validData_redirectsWithSuccess() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");

        mockMvc.perform(post("/finance/update-bank-details")
                .with(csrf())
                .param("accountHolderName", "John Doe")
                .param("accountNumber", "12345678")
                .param("sortCode", "12-34-56"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/finance/update-bank-details"))
            .andExpect(flash().attribute("success", "Bank details updated successfully."));

        verify(bankDetailsService).updateBankDetails(eq("CUST-1"), any(), any());
    }

    @Test
    @WithMockUser
    @DisplayName("POST with invalid data re-renders form with errors")
    void updateBankDetails_invalidData_reRendersForm() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");

        mockMvc.perform(post("/finance/update-bank-details")
                .with(csrf())
                .param("accountHolderName", "")
                .param("accountNumber", "123")
                .param("sortCode", "invalid"))
            .andExpect(status().isOk())
            .andExpect(view().name("finance/update-bank-details"));

        verify(bankDetailsService, never()).updateBankDetails(any(), any(), any());
    }

}
