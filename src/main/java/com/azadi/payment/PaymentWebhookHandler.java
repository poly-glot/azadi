package com.azadi.payment;

import com.azadi.audit.AuditService;
import com.azadi.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class PaymentWebhookHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentWebhookHandler.class);

    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    public PaymentWebhookHandler(PaymentRepository paymentRepository,
                                 EmailService emailService,
                                 AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    public void handlePaymentSuccess(String paymentIntentId, String webhookEventId, String ipAddress) {
        var existingEvent = paymentRepository.findByWebhookEventId(webhookEventId);
        if (existingEvent.isPresent()) {
            LOG.info("Duplicate webhook event ignored: {}", webhookEventId);
            return;
        }

        paymentRepository.findByStripePaymentIntentId(paymentIntentId).ifPresent(record -> {
            record.setStatus(PaymentRecord.STATUS_COMPLETED);
            record.setCompletedAt(Instant.now());
            record.setWebhookEventId(webhookEventId);
            paymentRepository.save(record);

            auditService.logEvent(record.getCustomerId(), "PAYMENT_COMPLETED", ipAddress,
                Map.of("amount", String.valueOf(record.getAmountPence()),
                    "paymentIntentId", paymentIntentId));

            emailService.sendPaymentConfirmation(
                record.getCustomerId(), record.getAmountPence());
        });
    }

    public void handlePaymentFailure(String paymentIntentId, String webhookEventId, String ipAddress) {
        paymentRepository.findByStripePaymentIntentId(paymentIntentId).ifPresent(record -> {
            record.setStatus(PaymentRecord.STATUS_FAILED);
            record.setCompletedAt(Instant.now());
            record.setWebhookEventId(webhookEventId);
            paymentRepository.save(record);

            auditService.logEvent(record.getCustomerId(), "PAYMENT_FAILED", ipAddress,
                Map.of("paymentIntentId", paymentIntentId));
        });
    }
}
