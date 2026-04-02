package com.azadi.statement;

import com.azadi.agreement.AgreementService;
import com.azadi.auth.AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StatementController {

    private final StatementService statementService;
    private final AgreementService agreementService;
    private final AuthorizationService authorizationService;

    public StatementController(StatementService statementService,
                               AgreementService agreementService,
                               AuthorizationService authorizationService) {
        this.statementService = statementService;
        this.agreementService = agreementService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/finance/request-a-statement")
    public String statementPage(Model model) {
        var customerId = authorizationService.getCurrentCustomerId();
        var agreements = agreementService.getAgreementsForCustomer(customerId);
        var statements = statementService.getStatementsForCustomer(customerId);
        model.addAttribute("agreements", agreements);
        model.addAttribute("statements", statements);
        return "finance/request-a-statement";
    }

    @PostMapping("/finance/request-a-statement")
    public String requestStatement(@RequestParam Long agreementId,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        var customerId = authorizationService.getCurrentCustomerId();
        statementService.requestStatement(customerId, agreementId, request.getRemoteAddr());
        redirectAttributes.addFlashAttribute("success", "Statement requested successfully. You will receive it by email.");
        return "redirect:/finance/request-a-statement";
    }
}
