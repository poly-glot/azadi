package com.azadi.bank;

import com.azadi.audit.AuditService;
import com.azadi.bank.dto.UpdateBankDetailsRequest;
import com.azadi.email.EmailService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
public class BankDetailsService {

    private final BankDetailsRepository bankDetailsRepository;
    private final BankDetailsEncryptor encryptor;
    private final AuditService auditService;
    private final EmailService emailService;

    public BankDetailsService(BankDetailsRepository bankDetailsRepository,
                              BankDetailsEncryptor encryptor,
                              AuditService auditService,
                              EmailService emailService) {
        this.bankDetailsRepository = bankDetailsRepository;
        this.encryptor = encryptor;
        this.auditService = auditService;
        this.emailService = emailService;
    }

    public Optional<BankDetails> getBankDetails(String customerId) {
        return bankDetailsRepository.findByCustomerId(customerId);
    }

    public BankDetails updateBankDetails(String customerId, UpdateBankDetailsRequest request,
                                         String ipAddress) {
        var bankDetails = bankDetailsRepository.findByCustomerId(customerId)
            .orElseGet(BankDetails::new);

        bankDetails.setCustomerId(customerId);
        bankDetails.setAccountHolderName(request.accountHolderName());
        bankDetails.setEncryptedAccountNumber(encryptor.encrypt(request.accountNumber()));
        bankDetails.setEncryptedSortCode(encryptor.encrypt(request.sortCode()));
        bankDetails.setLastFourAccount(request.accountNumber()
            .substring(request.accountNumber().length() - 4));
        bankDetails.setLastTwoSortCode(request.sortCode()
            .substring(request.sortCode().length() - 2));
        bankDetails.setUpdatedAt(Instant.now());

        var saved = bankDetailsRepository.save(bankDetails);

        auditService.logEvent(customerId, "BANK_DETAILS_UPDATED", ipAddress,
            Map.of("accountEnding", bankDetails.getLastFourAccount()));

        emailService.sendBankDetailsUpdated(customerId);

        return saved;
    }
}
