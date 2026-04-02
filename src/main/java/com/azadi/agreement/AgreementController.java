package com.azadi.agreement;

import com.azadi.agreement.dto.AgreementResponse;
import com.azadi.auth.AuthorizationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AgreementController {

    private final AgreementService agreementService;
    private final AuthorizationService authorizationService;

    public AgreementController(AgreementService agreementService,
                               AuthorizationService authorizationService) {
        this.agreementService = agreementService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/my-account")
    public String myAccount(Model model) {
        var customerId = authorizationService.getCurrentCustomerId();
        var agreements = agreementService.getAgreementsForCustomer(customerId)
            .stream()
            .map(AgreementResponse::from)
            .toList();

        model.addAttribute("agreements", agreements);
        return "my-account";
    }

    @GetMapping("/agreements/{id}")
    @PreAuthorize("@authorizationService.isOwner(#id)")
    public String agreementDetail(@PathVariable Long id, Model model) {
        var customerId = authorizationService.getCurrentCustomerId();
        var agreement = agreementService.getAgreement(customerId, id);
        model.addAttribute("agreement", AgreementResponse.from(agreement));
        return "agreement-detail";
    }
}
