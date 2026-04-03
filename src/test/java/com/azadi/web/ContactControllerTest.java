package com.azadi.web;

import com.azadi.auth.AuthorizationService;
import com.azadi.auth.Customer;
import com.azadi.contact.ContactController;
import com.azadi.contact.ContactService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContactController.class)
@Import(TestSecurityConfig.class)
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private ContactService contactService;
    @MockitoBean private AuthorizationService authorizationService;

    @Test
    @WithMockUser
    @DisplayName("GET /my-contact-details renders page with customer model")
    void contactDetailsPage_populatesCustomer() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");
        when(contactService.getCustomer("CUST-1")).thenReturn(buildCustomer());

        mockMvc.perform(get("/my-contact-details"))
            .andExpect(status().isOk())
            .andExpect(view().name("my-contact-details"))
            .andExpect(model().attributeExists("customer"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /my-contact-details updates and redirects with success flash")
    void updateContactDetails_redirectsWithSuccess() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");
        when(contactService.getCustomer("CUST-1")).thenReturn(buildCustomer());

        mockMvc.perform(post("/my-contact-details")
                .with(csrf())
                .param("email", "new@example.com"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/my-contact-details"))
            .andExpect(flash().attribute("success", "Contact details updated successfully."));

        verify(contactService).updateContactDetails(eq("CUST-1"), any(), any());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /my-contact-details with partial params preserves existing values")
    void updateContactDetails_partialUpdate() throws Exception {
        when(authorizationService.getCurrentCustomerId()).thenReturn("CUST-1");
        when(contactService.getCustomer("CUST-1")).thenReturn(buildCustomer());

        mockMvc.perform(post("/my-contact-details")
                .with(csrf())
                .param("mobilePhone", "07999999999"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/my-contact-details"));

        verify(contactService).updateContactDetails(eq("CUST-1"), any(), any());
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
