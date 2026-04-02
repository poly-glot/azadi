package com.azadi.settlement;

import com.azadi.agreement.AgreementService;
import com.azadi.auth.AuthorizationService;
import com.azadi.settlement.dto.SettlementResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SettlementController {

    private final SettlementService settlementService;
    private final AgreementService agreementService;
    private final AuthorizationService authorizationService;

    public SettlementController(SettlementService settlementService,
                                AgreementService agreementService,
                                AuthorizationService authorizationService) {
        this.settlementService = settlementService;
        this.agreementService = agreementService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/finance/settlement-figure")
    public String settlementPage(Model model) {
        var customerId = authorizationService.getCurrentCustomerId();
        var agreements = agreementService.getAgreementsForCustomer(customerId);
        var settlements = settlementService.getSettlementsForCustomer(customerId)
            .stream()
            .map(SettlementResponse::from)
            .toList();
        model.addAttribute("agreements", agreements);
        model.addAttribute("settlements", settlements);
        return "finance/settlement-figure";
    }

    @PostMapping(value = "/finance/settlement-figure", params = "agreementId")
    public String calculateSettlement(@RequestParam Long agreementId, Model model) {
        var customerId = authorizationService.getCurrentCustomerId();
        var agreements = agreementService.getAgreementsForCustomer(customerId);
        model.addAttribute("agreements", agreements);

        var figure = settlementService.calculateSettlement(customerId, agreementId);
        model.addAttribute("settlement", SettlementResponse.from(figure));
        return "finance/settlement-figure";
    }

    @PostMapping(value = "/finance/settlement-figure", params = "mobileNumber")
    public String sendSettlementSms(@RequestParam String mobileNumber,
                                    RedirectAttributes redirectAttributes) {
        // SMS sending would be implemented here
        redirectAttributes.addFlashAttribute("smsSent", true);
        return "redirect:/finance/settlement-figure";
    }
}
