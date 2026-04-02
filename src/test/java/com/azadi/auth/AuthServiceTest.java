package com.azadi.auth;

import com.azadi.agreement.Agreement;
import com.azadi.agreement.AgreementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AgreementRepository agreementRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LoginAttemptTracker loginAttemptTracker;

    private AzadiAuthenticationProvider authenticationProvider;

    private static final String AGREEMENT_NUMBER = "AGR-001";
    private static final LocalDate DOB = LocalDate.of(1990, 5, 15);
    private static final String DOB_STRING = "15/05/1990";
    private static final String POSTCODE = "SW1A 1AA";
    private static final String CUSTOMER_ID = "CUST-001";

    @BeforeEach
    void setUp() {
        authenticationProvider = new AzadiAuthenticationProvider(
            agreementRepository, customerRepository, loginAttemptTracker);
    }

    @Test
    @DisplayName("Should authenticate successfully with correct credentials")
    void authenticateSuccessfully() {
        // Arrange
        Agreement agreement = buildAgreement(AGREEMENT_NUMBER, CUSTOMER_ID);
        Customer customer = buildCustomer(CUSTOMER_ID, DOB, POSTCODE);

        when(loginAttemptTracker.isBlocked(AGREEMENT_NUMBER)).thenReturn(false);
        when(agreementRepository.findByAgreementNumber(AGREEMENT_NUMBER))
            .thenReturn(Optional.of(agreement));
        when(customerRepository.findByCustomerId(CUSTOMER_ID))
            .thenReturn(Optional.of(customer));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                AGREEMENT_NUMBER, DOB_STRING + "|" + POSTCODE);

        // Act
        Authentication result = authenticationProvider.authenticate(auth);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isInstanceOf(CustomUserDetails.class);
        var details = (CustomUserDetails) result.getPrincipal();
        assertThat(details.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(details.agreementNumber()).isEqualTo(AGREEMENT_NUMBER);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException for wrong date of birth")
    void wrongDobThrowsBadCredentials() {
        // Arrange
        Agreement agreement = buildAgreement(AGREEMENT_NUMBER, CUSTOMER_ID);
        Customer customer = buildCustomer(CUSTOMER_ID, DOB, POSTCODE);

        when(loginAttemptTracker.isBlocked(AGREEMENT_NUMBER)).thenReturn(false);
        when(agreementRepository.findByAgreementNumber(AGREEMENT_NUMBER))
            .thenReturn(Optional.of(agreement));
        when(customerRepository.findByCustomerId(CUSTOMER_ID))
            .thenReturn(Optional.of(customer));

        String wrongDob = "01/01/1985";
        Authentication auth = new UsernamePasswordAuthenticationToken(
                AGREEMENT_NUMBER, wrongDob + "|" + POSTCODE);

        // Act & Assert
        assertThatThrownBy(() -> authenticationProvider.authenticate(auth))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid agreement number, date of birth, or postcode");
    }

    @Test
    @DisplayName("Should throw BadCredentialsException for wrong postcode")
    void wrongPostcodeThrowsBadCredentials() {
        // Arrange
        Agreement agreement = buildAgreement(AGREEMENT_NUMBER, CUSTOMER_ID);
        Customer customer = buildCustomer(CUSTOMER_ID, DOB, POSTCODE);

        when(loginAttemptTracker.isBlocked(AGREEMENT_NUMBER)).thenReturn(false);
        when(agreementRepository.findByAgreementNumber(AGREEMENT_NUMBER))
            .thenReturn(Optional.of(agreement));
        when(customerRepository.findByCustomerId(CUSTOMER_ID))
            .thenReturn(Optional.of(customer));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                AGREEMENT_NUMBER, DOB_STRING + "|M1 1AE");

        // Act & Assert
        assertThatThrownBy(() -> authenticationProvider.authenticate(auth))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid agreement number, date of birth, or postcode");
    }

    @Test
    @DisplayName("Should throw BadCredentialsException for non-existent agreement")
    void nonExistentAgreementThrowsBadCredentials() {
        // Arrange
        when(loginAttemptTracker.isBlocked("AGR-999")).thenReturn(false);
        when(agreementRepository.findByAgreementNumber("AGR-999"))
            .thenReturn(Optional.empty());

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "AGR-999", DOB_STRING + "|" + POSTCODE);

        // Act & Assert
        assertThatThrownBy(() -> authenticationProvider.authenticate(auth))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid agreement number, date of birth, or postcode");
    }

    @Test
    @DisplayName("Should match postcode case-insensitively")
    void caseInsensitivePostcodeMatching() {
        // Arrange
        Agreement agreement = buildAgreement(AGREEMENT_NUMBER, CUSTOMER_ID);
        Customer customer = buildCustomer(CUSTOMER_ID, DOB, "SW1A 1AA");

        when(loginAttemptTracker.isBlocked(AGREEMENT_NUMBER)).thenReturn(false);
        when(agreementRepository.findByAgreementNumber(AGREEMENT_NUMBER))
            .thenReturn(Optional.of(agreement));
        when(customerRepository.findByCustomerId(CUSTOMER_ID))
            .thenReturn(Optional.of(customer));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                AGREEMENT_NUMBER, DOB_STRING + "|sw1a 1aa");

        // Act
        Authentication result = authenticationProvider.authenticate(auth);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when account is locked")
    void lockedAccountThrowsBadCredentials() {
        // Arrange
        when(loginAttemptTracker.isBlocked(AGREEMENT_NUMBER)).thenReturn(true);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                AGREEMENT_NUMBER, DOB_STRING + "|" + POSTCODE);

        // Act & Assert
        assertThatThrownBy(() -> authenticationProvider.authenticate(auth))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Account temporarily locked");
    }

    @Test
    @DisplayName("supports UsernamePasswordAuthenticationToken")
    void supportsUsernamePasswordAuthenticationToken() {
        assertThat(authenticationProvider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
    }

    private Agreement buildAgreement(String agreementNumber, String customerId) {
        var agreement = new Agreement();
        agreement.setAgreementNumber(agreementNumber);
        agreement.setCustomerId(customerId);
        return agreement;
    }

    private Customer buildCustomer(String customerId, LocalDate dob, String postcode) {
        var customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setDob(dob);
        customer.setPostcode(postcode);
        customer.setFullName("Test Customer");
        return customer;
    }
}
