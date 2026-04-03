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
        return getAuthenticatedUserDetails().customerId();
    }

    public String getCurrentCustomerName() {
        return getAuthenticatedUserDetails().customerName();
    }

    private CustomUserDetails getAuthenticatedUserDetails() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details;
        }
        throw new IllegalStateException("No authenticated user found.");
    }
}
