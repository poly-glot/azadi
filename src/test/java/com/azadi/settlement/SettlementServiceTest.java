package com.azadi.settlement;

import com.azadi.agreement.Agreement;
import com.azadi.agreement.AgreementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private AgreementService agreementService;

    private SettlementService settlementService;

    private static final String CUSTOMER_ID = "CUST-001";
    private static final Long AGREEMENT_ID = 1L;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(settlementRepository, agreementService);
    }

    @Test
    @DisplayName("calculateSettlement returns correct figure with 2% early settlement fee")
    void calculateSettlementReturnsCorrectFigure() {
        // Arrange
        var agreement = new Agreement();
        agreement.setBalancePence(1200000L); // £12,000
        when(agreementService.getAgreement(CUSTOMER_ID, AGREEMENT_ID)).thenReturn(agreement);
        when(settlementRepository.save(any(SettlementFigure.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // Act
        var result = settlementService.calculateSettlement(CUSTOMER_ID, AGREEMENT_ID);

        // Assert
        assertThat(result.getAmountPence()).isEqualTo(1224000L); // 1200000 + 2% = 1224000
        assertThat(result.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(result.getAgreementId()).isEqualTo(AGREEMENT_ID);
        assertThat(result.getValidUntil()).isNotNull();
    }

    @Test
    @DisplayName("calculateSettlement saves to repository")
    void calculateSettlementSavesToRepository() {
        // Arrange
        var agreement = new Agreement();
        agreement.setBalancePence(500000L);
        when(agreementService.getAgreement(CUSTOMER_ID, AGREEMENT_ID)).thenReturn(agreement);
        when(settlementRepository.save(any(SettlementFigure.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // Act
        settlementService.calculateSettlement(CUSTOMER_ID, AGREEMENT_ID);

        // Assert
        ArgumentCaptor<SettlementFigure> captor = ArgumentCaptor.forClass(SettlementFigure.class);
        verify(settlementRepository).save(captor.capture());
        assertThat(captor.getValue().getAmountPence()).isEqualTo(510000L);
    }

    @Test
    @DisplayName("getSettlementsForCustomer delegates to repository")
    void getSettlementsForCustomerDelegatesToRepository() {
        // Act
        settlementService.getSettlementsForCustomer(CUSTOMER_ID);

        // Assert
        verify(settlementRepository).findByCustomerId(CUSTOMER_ID);
    }
}
