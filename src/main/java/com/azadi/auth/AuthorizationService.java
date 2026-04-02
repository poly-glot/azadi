package com.azadi.auth;

import com.azadi.agreement.AgreementRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationService {

    private final AgreementRepository agreementRepository;

    public AuthorizationService(AgreementRepository agreementRepository) {
        this.agreementRepository = agreementRepository;
    }

    public boolean isOwner(Long agreementId) {
        var customerId = getCurrentCustomerId();
        return agreementRepository.findById(agreementId)
            .map(agreement -> customerId.equals(agreement.getCustomerId()))
            .orElse(false);
    }

    public String getCurrentCustomerId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details.customerId();
        }
        throw new IllegalStateException("No authenticated user found.");
    }

    public String getCurrentCustomerName() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details.customerName();
        }
        throw new IllegalStateException("No authenticated user found.");
    }
}
