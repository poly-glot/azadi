package com.azadi.agreement;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AgreementService {

    private final AgreementRepository agreementRepository;

    public AgreementService(AgreementRepository agreementRepository) {
        this.agreementRepository = agreementRepository;
    }

    public List<Agreement> getAgreementsForCustomer(String customerId) {
        return agreementRepository.findByCustomerId(customerId);
    }

    public Agreement getAgreement(String customerId, Long agreementId) {
        var agreement = agreementRepository.findById(agreementId)
            .orElseThrow(() -> new NoSuchElementException("Agreement not found: " + agreementId));

        if (!customerId.equals(agreement.getCustomerId())) {
            throw new AccessDeniedException("You do not have access to this agreement.");
        }

        return agreement;
    }
}
