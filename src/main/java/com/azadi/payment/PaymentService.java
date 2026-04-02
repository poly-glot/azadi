package com.azadi.payment;

import com.azadi.audit.AuditService;
import com.azadi.auth.AuthorizationService;
import com.azadi.email.EmailService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

    private final StripePaymentService stripePaymentService;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    private final AuditService auditService;
    private final AuthorizationService authorizationService;

    public PaymentService(StripePaymentService stripePaymentService,
                          PaymentRepository paymentRepository,
                          EmailService emailService,
                          AuditService auditService,
                          AuthorizationService authorizationService) {
        this.stripePaymentService = stripePaymentService;
        this.paymentRepository = paymentRepository;
        this.emailService = emailService;
        this.auditService = auditService;
        this.authorizationService = authorizationService;
    }

    public PaymentIntent initiatePayment(long amountPence, Long agreementId,
                                         String agreementNumber, String customerEmail,
                                         String ipAddress) throws StripeException {
        var customerId = authorizationService.getCurrentCustomerId();
        var paymentIntent = stripePaymentService.createPaymentIntent(amountPence, agreementNumber, customerEmail);

        var record = new PaymentRecord();
        record.setAgreementId(agreementId);
        record.setCustomerId(customerId);
        record.setAmountPence(amountPence);
        record.setStripePaymentIntentId(paymentIntent.getId());
        record.setStatus(PaymentRecord.STATUS_PENDING);
        record.setCreatedAt(Instant.now());
        paymentRepository.save(record);

        auditService.logEvent(customerId, "PAYMENT_INITIATED", ipAddress,
            Map.of("amount", String.valueOf(amountPence), "agreementNumber", agreementNumber));

        return paymentIntent;
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
