package com.azadi.payment;

import com.azadi.agreement.AgreementService;
import com.azadi.auth.AuthorizationService;
import com.azadi.payment.dto.MakePaymentRequest;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

    private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final StripePaymentService stripePaymentService;
    private final AgreementService agreementService;
    private final AuthorizationService authorizationService;
    private final String stripePublishableKey;

    public PaymentController(PaymentService paymentService,
                             StripePaymentService stripePaymentService,
                             AgreementService agreementService,
                             AuthorizationService authorizationService,
                             @Value("${stripe.publishable-key}") String stripePublishableKey) {
        this.paymentService = paymentService;
        this.stripePaymentService = stripePaymentService;
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
            HttpServletRequest httpRequest) {
        try {
            var customerId = authorizationService.getCurrentCustomerId();
            var agreement = agreementService.getAgreement(customerId, request.agreementId());
            var ipAddress = httpRequest.getRemoteAddr();

            var paymentIntent = paymentService.initiatePayment(
                request.amountPence(),
                agreement.getId(),
                agreement.getAgreementNumber(),
                "", // email resolved from customer record
                ipAddress
            );

            return ResponseEntity.ok(Map.of("clientSecret", paymentIntent.getClientSecret()));
        } catch (StripeException e) {
            LOG.error("Stripe error creating payment intent", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Payment service unavailable. Please try again."));
        }
    }

    @PostMapping("/api/stripe/webhook")
    @ResponseBody
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader,
            HttpServletRequest request) {
        try {
            var event = stripePaymentService.constructWebhookEvent(payload, sigHeader);
            var ipAddress = request.getRemoteAddr();

            switch (event.getType()) {
                case "payment_intent.succeeded" -> {
                    var paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElse(null);
                    if (paymentIntent != null) {
                        paymentService.handlePaymentSuccess(paymentIntent.getId(), event.getId(), ipAddress);
                    }
                }
                case "payment_intent.payment_failed" -> {
                    var paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElse(null);
                    if (paymentIntent != null) {
                        paymentService.handlePaymentFailure(paymentIntent.getId(), event.getId(), ipAddress);
                    }
                }
                default -> LOG.info("Unhandled webhook event type: {}", event.getType());
            }

            return ResponseEntity.ok("ok");
        } catch (SignatureVerificationException e) {
            LOG.warn("Invalid webhook signature", e);
            return ResponseEntity.badRequest().body("Invalid signature");
        }
    }
}
