package com.azadi.bank;

import com.azadi.auth.AuthorizationService;
import com.azadi.bank.dto.UpdateBankDetailsRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class BankDetailsController {

    private final BankDetailsService bankDetailsService;
    private final AuthorizationService authorizationService;

    public BankDetailsController(BankDetailsService bankDetailsService,
                                 AuthorizationService authorizationService) {
        this.bankDetailsService = bankDetailsService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/finance/update-bank-details")
    public String bankDetailsForm(Model model) {
        var customerId = authorizationService.getCurrentCustomerId();
        var existing = bankDetailsService.getBankDetails(customerId);
        existing.ifPresent(details -> model.addAttribute("bankDetails", details));
        return "finance/update-bank-details";
    }

    @PostMapping("/finance/update-bank-details")
    public String updateBankDetails(@Valid @ModelAttribute UpdateBankDetailsRequest request,
                                    BindingResult bindingResult,
                                    HttpServletRequest httpRequest,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            return "finance/update-bank-details";
        }

        var customerId = authorizationService.getCurrentCustomerId();
        var ipAddress = httpRequest.getRemoteAddr();
        bankDetailsService.updateBankDetails(customerId, request, ipAddress);

        redirectAttributes.addFlashAttribute("success", "Bank details updated successfully.");
        return "redirect:/finance/update-bank-details";
    }
}
