package com.azadi.agreement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgreementServiceTest {

    @Mock
    private AgreementRepository agreementRepository;

    private AgreementService agreementService;

    private static final String CUSTOMER_ID = "CUST-100";
    private static final String OTHER_CUSTOMER_ID = "CUST-200";

    @BeforeEach
    void setUp() {
        agreementService = new AgreementService(agreementRepository);
    }

    @Test
    @DisplayName("getAgreementsForCustomer returns correct list of agreements")
    void getAgreementsForCustomerReturnsCorrectList() {
        // Arrange
        Agreement agreement1 = buildAgreement(1L, "AGR-001", CUSTOMER_ID);
        Agreement agreement2 = buildAgreement(2L, "AGR-002", CUSTOMER_ID);
        when(agreementRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(agreement1, agreement2));

        // Act
        List<Agreement> agreements = agreementService.getAgreementsForCustomer(CUSTOMER_ID);

        // Assert
        assertThat(agreements).hasSize(2);
        assertThat(agreements.get(0).getAgreementNumber()).isEqualTo("AGR-001");
        assertThat(agreements.get(1).getAgreementNumber()).isEqualTo("AGR-002");
    }

    @Test
    @DisplayName("getAgreement returns agreement when customer owns it")
    void getAgreementWithValidOwnership() {
        // Arrange
        Agreement agreement = buildAgreement(1L, "AGR-001", CUSTOMER_ID);
        when(agreementRepository.findById(1L)).thenReturn(Optional.of(agreement));

        // Act
        Agreement result = agreementService.getAgreement(CUSTOMER_ID, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAgreementNumber()).isEqualTo("AGR-001");
        assertThat(result.getCustomerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    @DisplayName("getAgreement throws AccessDeniedException when customer does not own agreement")
    void getAgreementWithWrongCustomerThrowsAccessDenied() {
        // Arrange
        Agreement agreement = buildAgreement(1L, "AGR-001", OTHER_CUSTOMER_ID);
        when(agreementRepository.findById(1L)).thenReturn(Optional.of(agreement));

        // Act & Assert
        assertThatThrownBy(() -> agreementService.getAgreement(CUSTOMER_ID, 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have access to this agreement");
    }

    @Test
    @DisplayName("getAgreement throws NoSuchElementException when agreement not found")
    void getAgreementNotFoundThrowsException() {
        // Arrange
        when(agreementRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> agreementService.getAgreement(CUSTOMER_ID, 999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Agreement not found: 999");
    }

    private Agreement buildAgreement(Long id, String agreementNumber, String customerId) {
        var agreement = new Agreement();
        agreement.setId(id);
        agreement.setAgreementNumber(agreementNumber);
        agreement.setCustomerId(customerId);
        agreement.setVehicleModel("2024 Test Vehicle");
        return agreement;
    }
}
