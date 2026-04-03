package com.azadi.payment;

import com.azadi.agreement.AgreementService;
import com.azadi.auth.AuthorizationService;
import com.azadi.payment.dto.MakePaymentRequest;
import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentWebhookHandler webhookHandler;
    private final AgreementService agreementService;
    private final AuthorizationService authorizationService;
    private final String stripePublishableKey;

    public PaymentController(PaymentService paymentService,
                             PaymentWebhookHandler webhookHandler,
                             AgreementService agreementService,
                             AuthorizationService authorizationService,
                             @Value("${stripe.publishable-key}") String stripePublishableKey) {
        this.paymentService = paymentService;
        this.webhookHandler = webhookHandler;
        this.agreementService = agreementService;
        this.authorizationService = authorizationService;
        this.stripePublishableKey = stripePublishableKey;
    }

    @GetMapping("/finance/make-a-payment")
    public String makePaymentPage(Model model) {
        var customerId = authorizationService.getCurrentCustomerId();
        var agreements = agreementService.getAgreementsForCustomer(customerId);
        model.addAttribute("agreements", agreements);
        model.addAttribute("stripePublishableKey", stripePublishableKey);
        return "finance/make-a-payment";
    }

    @PostMapping("/finance/make-a-payment")
    @ResponseBody
    public ResponseEntity<Map<String, String>> makePayment(
            @Valid @RequestBody MakePaymentRequest request,
            HttpServletRequest httpRequest) throws StripeException {
        var customerId = authorizationService.getCurrentCustomerId();
        var ipAddress = httpRequest.getRemoteAddr();

        var paymentIntent = paymentService.initiatePayment(
            customerId, request.agreementId(), request.amountPence(), ipAddress);

        return ResponseEntity.ok(Map.of("clientSecret", paymentIntent.getClientSecret()));
    }

    @PostMapping("/api/stripe/webhook")
    @ResponseBody
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader,
            HttpServletRequest request) {
        return webhookHandler.handleEvent(payload, sigHeader, request.getRemoteAddr());
    }
}
