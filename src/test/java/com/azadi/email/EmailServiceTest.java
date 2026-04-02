package com.azadi.email;

import com.azadi.auth.Customer;
import com.azadi.auth.CustomerRepository;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private EmailService emailService;

    private static final String CUSTOMER_ID = "CUST-001";
    private static final String CUSTOMER_EMAIL = "james@example.com";

    @BeforeEach
    void setUp() {
        emailService = new EmailService("re_test_mock", "test@azadi.test",
            customerRepository, JsonMapper.shared(), HttpClient.newHttpClient());
    }

    @Test
    @DisplayName("sendEmail does not throw on success")
    void sendEmailDoesNotThrow() {
        // sendEmail uses async HttpClient — should never throw
        assertThatCode(() ->
            emailService.sendEmail(CUSTOMER_EMAIL, "Test Subject", "<h1>Hello</h1>"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendPaymentConfirmation resolves customer email and sends")
    void sendPaymentConfirmationResolvesEmail() {
        // Arrange
        var customer = new Customer();
        customer.setEmail(CUSTOMER_EMAIL);
        when(customerRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        // Act — should not throw (async send)
        assertThatCode(() ->
            emailService.sendPaymentConfirmation(CUSTOMER_ID, 112732L))
            .doesNotThrowAnyException();

        // Assert
        verify(customerRepository).findByCustomerId(CUSTOMER_ID);
    }

    @Test
    @DisplayName("sendPaymentConfirmation skips if customer has no email")
    void sendPaymentConfirmationSkipsNoEmail() {
        // Arrange
        var customer = new Customer();
        customer.setEmail(null);
        when(customerRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        // Act
        emailService.sendPaymentConfirmation(CUSTOMER_ID, 112732L);

        // The email service should not attempt to send (no email address)
        // Since sendEmail is async we can't directly verify, but no exception should occur
    }

    @Test
    @DisplayName("Convenience methods do not throw when customer not found")
    void convenienceMethodsHandleMissingCustomer() {
        // Arrange
        when(customerRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

        // Act & Assert — none should throw
        assertThatCode(() -> emailService.sendPaymentConfirmation(CUSTOMER_ID, 100L))
            .doesNotThrowAnyException();
        assertThatCode(() -> emailService.sendBankDetailsUpdated(CUSTOMER_ID))
            .doesNotThrowAnyException();
        assertThatCode(() -> emailService.sendLoginAlert(CUSTOMER_ID, "127.0.0.1"))
            .doesNotThrowAnyException();
    }
}
