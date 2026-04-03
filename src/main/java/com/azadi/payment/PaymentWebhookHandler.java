package com.azadi.payment;

import com.azadi.audit.AuditService;
import com.azadi.email.EmailService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class PaymentWebhookHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentWebhookHandler.class);

    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    private final AuditService auditService;
    private final StripePaymentService stripePaymentService;

    public PaymentWebhookHandler(PaymentRepository paymentRepository,
                                 EmailService emailService,
                                 AuditService auditService,
                                 StripePaymentService stripePaymentService) {
        this.paymentRepository = paymentRepository;
        this.emailService = emailService;
        this.auditService = auditService;
        this.stripePaymentService = stripePaymentService;
    }

    public ResponseEntity<String> handleEvent(String payload, String sigHeader, String ipAddress) {
        try {
            var event = stripePaymentService.constructWebhookEvent(payload, sigHeader);

            switch (event.getType()) {
                case "payment_intent.succeeded" -> event.getDataObjectDeserializer()
                    .getObject()
                    .filter(PaymentIntent.class::isInstance)
                    .map(PaymentIntent.class::cast)
                    .ifPresent(pi -> handlePaymentSuccess(pi.getId(), event.getId(), ipAddress));
                case "payment_intent.payment_failed" -> event.getDataObjectDeserializer()
                    .getObject()
                    .filter(PaymentIntent.class::isInstance)
                    .map(PaymentIntent.class::cast)
                    .ifPresent(pi -> handlePaymentFailure(pi.getId(), event.getId(), ipAddress));
                default -> LOG.info("Unhandled webhook event type: {}", event.getType());
            }

            return ResponseEntity.ok("ok");
        } catch (SignatureVerificationException e) {
            LOG.warn("Invalid webhook signature", e);
            return ResponseEntity.badRequest().body("Invalid signature");
        }
    }

    private void handlePaymentSuccess(String paymentIntentId, String webhookEventId, String ipAddress) {
        var existingEvent = paymentRepository.findByWebhookEventId(webhookEventId);
        if (existingEvent.isPresent()) {
            LOG.info("Duplicate webhook event ignored: {}", webhookEventId);
            return;
        }

        paymentRepository.findByStripePaymentIntentId(paymentIntentId).ifPresentOrElse(
            record -> {
                record.setStatus(PaymentRecord.STATUS_COMPLETED);
                record.setCompletedAt(Instant.now());
                record.setWebhookEventId(webhookEventId);
                paymentRepository.save(record);

                auditService.logEvent(record.getCustomerId(), "PAYMENT_COMPLETED", ipAddress,
                    Map.of("amount", String.valueOf(record.getAmountPence()),
                        "paymentIntentId", paymentIntentId));

                emailService.sendPaymentConfirmation(
                    record.getCustomerId(), record.getAmountPence());
            },
            () -> LOG.warn("No PaymentRecord found for PaymentIntent: {}", paymentIntentId)
        );
    }

    private void handlePaymentFailure(String paymentIntentId, String webhookEventId, String ipAddress) {
        paymentRepository.findByStripePaymentIntentId(paymentIntentId).ifPresentOrElse(
            record -> {
                record.setStatus(PaymentRecord.STATUS_FAILED);
                record.setCompletedAt(Instant.now());
                record.setWebhookEventId(webhookEventId);
                paymentRepository.save(record);

                auditService.logEvent(record.getCustomerId(), "PAYMENT_FAILED", ipAddress,
                    Map.of("paymentIntentId", paymentIntentId));
            },
            () -> LOG.warn("No PaymentRecord found for PaymentIntent: {}", paymentIntentId)
        );
    }
}
