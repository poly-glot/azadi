package com.azadi.bank;

import com.azadi.audit.AuditService;
import com.azadi.bank.dto.UpdateBankDetailsRequest;
import com.azadi.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankDetailsServiceTest {

    @Mock
    private BankDetailsRepository bankDetailsRepository;

    @Mock
    private BankDetailsEncryptor encryptor;

    @Mock
    private AuditService auditService;

    @Mock
    private EmailService emailService;

    private BankDetailsService bankDetailsService;

    private static final String CUSTOMER_ID = "CUST-001";
    private static final String SORT_CODE = "12-34-56";
    private static final String ACCOUNT_NUMBER = "12345678";
    private static final String ACCOUNT_NAME = "John Doe";

    @BeforeEach
    void setUp() {
        bankDetailsService = new BankDetailsService(
            bankDetailsRepository, encryptor, auditService, emailService);
    }

    @Test
    @DisplayName("updateBankDetails encrypts sort code and account number before saving")
    void updateBankDetailsEncryptsBeforeSave() {
        // Arrange
        when(bankDetailsRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());
        when(encryptor.encrypt(SORT_CODE)).thenReturn("encrypted-sort");
        when(encryptor.encrypt(ACCOUNT_NUMBER)).thenReturn("encrypted-acct");
        when(bankDetailsRepository.save(any(BankDetails.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateBankDetailsRequest(ACCOUNT_NAME, ACCOUNT_NUMBER, SORT_CODE);

        // Act
        bankDetailsService.updateBankDetails(CUSTOMER_ID, request, "127.0.0.1");

        // Assert
        ArgumentCaptor<BankDetails> captor = ArgumentCaptor.forClass(BankDetails.class);
        verify(bankDetailsRepository).save(captor.capture());

        var saved = captor.getValue();
        assertThat(saved.getEncryptedSortCode()).isEqualTo("encrypted-sort");
        assertThat(saved.getEncryptedAccountNumber()).isEqualTo("encrypted-acct");
        assertThat(saved.getAccountHolderName()).isEqualTo(ACCOUNT_NAME);
        assertThat(saved.getLastFourAccount()).isEqualTo("5678");
        assertThat(saved.getLastTwoSortCode()).isEqualTo("56");
    }

    @Test
    @DisplayName("updateBankDetails sends email and audit event")
    void updateBankDetailsSendsEmailAndAudit() {
        // Arrange
        when(bankDetailsRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());
        when(encryptor.encrypt(any())).thenReturn("encrypted");
        when(bankDetailsRepository.save(any(BankDetails.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateBankDetailsRequest(ACCOUNT_NAME, ACCOUNT_NUMBER, SORT_CODE);

        // Act
        bankDetailsService.updateBankDetails(CUSTOMER_ID, request, "127.0.0.1");

        // Assert
        verify(emailService).sendBankDetailsUpdated(CUSTOMER_ID);
        verify(auditService).logEvent(
            org.mockito.ArgumentMatchers.eq(CUSTOMER_ID),
            org.mockito.ArgumentMatchers.eq("BANK_DETAILS_UPDATED"),
            org.mockito.ArgumentMatchers.eq("127.0.0.1"),
            any());
    }

    @Test
    @DisplayName("getBankDetails delegates to repository")
    void getBankDetailsDelegatesToRepository() {
        // Act
        bankDetailsService.getBankDetails(CUSTOMER_ID);

        // Assert
        verify(bankDetailsRepository).findByCustomerId(CUSTOMER_ID);
    }
}
