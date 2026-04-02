package com.azadi.payment;

import com.azadi.agreement.AgreementService;
import com.azadi.auth.AuthorizationService;
import com.azadi.common.OrdinalFormat;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PaymentDateController {

    private final AgreementService agreementService;
    private final AuthorizationService authorizationService;
    private final PaymentDateService paymentDateService;

    public PaymentDateController(AgreementService agreementService,
                                 AuthorizationService authorizationService,
                                 PaymentDateService paymentDateService) {
        this.agreementService = agreementService;
        this.authorizationService = authorizationService;
        this.paymentDateService = paymentDateService;
    }

    @GetMapping("/finance/change-payment-date")
    public String changePaymentDatePage(Model model) {
        var customerId = authorizationService.getCurrentCustomerId();
        var agreements = agreementService.getAgreementsForCustomer(customerId);
        model.addAttribute("agreements", agreements);

        if (!agreements.isEmpty()) {
            var first = agreements.getFirst();
            int day = paymentDateService.getCurrentPaymentDay(first);
            model.addAttribute("currentPaymentDay", day);
            model.addAttribute("currentPaymentDate", OrdinalFormat.dayWithSuffix(day));
            model.addAttribute("alreadyChanged", first.isPaymentDateChanged());
        }
        return "finance/change-payment-date";
    }

    @PostMapping("/finance/change-payment-date")
    public String changePaymentDate(@RequestParam int newPaymentDate,
                                    @RequestParam Long agreementId,
                                    HttpServletRequest httpRequest,
                                    RedirectAttributes redirectAttributes) {
        var customerId = authorizationService.getCurrentCustomerId();

        try {
            paymentDateService.changePaymentDate(customerId, agreementId, newPaymentDate, httpRequest.getRemoteAddr());
            redirectAttributes.addFlashAttribute("success", true);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/finance/change-payment-date";
    }
}
