package com.azadi.auth;

import com.azadi.agreement.Agreement;
import com.azadi.agreement.AgreementRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private AgreementRepository agreementRepository;

    private AuthorizationService authorizationService;

    private static final String CUSTOMER_ID = "CUST-001";
    private static final String OTHER_CUSTOMER_ID = "CUST-002";
    private static final Long AGREEMENT_ID = 1L;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(agreementRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("isOwner returns true when customerId matches authenticated user")
    void isOwnerReturnsTrueForMatchingCustomerId() {
        // Arrange
        setUpSecurityContext(CUSTOMER_ID);

        var agreement = new Agreement();
        agreement.setCustomerId(CUSTOMER_ID);
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));

        // Act
        boolean result = authorizationService.isOwner(AGREEMENT_ID);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isOwner returns false when customerId does not match")
    void isOwnerReturnsFalseForDifferentCustomerId() {
        // Arrange
        setUpSecurityContext(CUSTOMER_ID);

        var agreement = new Agreement();
        agreement.setCustomerId(OTHER_CUSTOMER_ID);
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));

        // Act
        boolean result = authorizationService.isOwner(AGREEMENT_ID);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isOwner returns false when agreement not found")
    void isOwnerReturnsFalseWhenAgreementNotFound() {
        // Arrange
        setUpSecurityContext(CUSTOMER_ID);
        when(agreementRepository.findById(AGREEMENT_ID)).thenReturn(Optional.empty());

        // Act
        boolean result = authorizationService.isOwner(AGREEMENT_ID);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getCurrentCustomerId throws IllegalStateException when no user authenticated")
    void getCurrentCustomerIdThrowsWhenNoAuth() {
        // Act & Assert
        assertThatThrownBy(() -> authorizationService.getCurrentCustomerId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated user found");
    }

    @Test
    @DisplayName("getCurrentCustomerName returns name from security context")
    void getCurrentCustomerNameReturnsName() {
        // Arrange
        setUpSecurityContext(CUSTOMER_ID);

        // Act
        String name = authorizationService.getCurrentCustomerName();

        // Assert
        assertThat(name).isEqualTo("Test Customer");
    }

    private void setUpSecurityContext(String customerId) {
        var userDetails = new CustomUserDetails(customerId, "Test Customer", "AGR-001");
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
