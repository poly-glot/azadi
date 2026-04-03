package com.azadi.payment;

import com.azadi.agreement.Agreement;
import com.azadi.agreement.AgreementRepository;
import com.azadi.agreement.AgreementService;
import com.azadi.audit.AuditService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PaymentDateService {

    private final AgreementRepository agreementRepository;
    private final AgreementService agreementService;
    private final AuditService auditService;

    public PaymentDateService(AgreementRepository agreementRepository,
                              AgreementService agreementService,
                              AuditService auditService) {
        this.agreementRepository = agreementRepository;
        this.agreementService = agreementService;
        this.auditService = auditService;
    }

    public void changePaymentDate(String customerId, Long agreementId, int newDay, String ipAddress) {
        var agreement = agreementService.getAgreement(customerId, agreementId);

        if (agreement.isPaymentDateChanged()) {
            throw new IllegalStateException("Payment date has already been changed for this agreement.");
        }

        if (newDay < 1 || newDay > 28) {
            throw new IllegalArgumentException("Payment day must be between 1 and 28.");
        }

        var currentNext = agreement.getNextPaymentDate();
        if (currentNext != null) {
            agreement.setNextPaymentDate(currentNext.withDayOfMonth(newDay));
        }
        agreement.setPaymentDateChanged(true);
        agreementRepository.save(agreement);

        auditService.logEvent(customerId, "PAYMENT_DATE_CHANGED", ipAddress,
            Map.of("agreementId", String.valueOf(agreementId),
                   "newDay", String.valueOf(newDay)));
    }

    public int getCurrentPaymentDay(Agreement agreement) {
        return agreement.getNextPaymentDate() != null
            ? agreement.getNextPaymentDate().getDayOfMonth()
            : 1;
    }
}
