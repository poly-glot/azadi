package com.azadi.payment;

import com.azadi.audit.AuditService;
import com.azadi.auth.CustomerRepository;
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
    private final AuditService auditService;
    private final CustomerRepository customerRepository;

    public PaymentService(StripePaymentService stripePaymentService,
                          PaymentRepository paymentRepository,
                          AuditService auditService,
                          CustomerRepository customerRepository) {
        this.stripePaymentService = stripePaymentService;
        this.paymentRepository = paymentRepository;
        this.auditService = auditService;
        this.customerRepository = customerRepository;
    }

    public PaymentIntent initiatePayment(long amountPence, Long agreementId,
                                         String agreementNumber, String customerId,
                                         String ipAddress) throws StripeException {
        var customerEmail = customerRepository.findByCustomerId(customerId)
            .map(c -> c.getEmail() != null ? c.getEmail() : "")
            .orElse("");
        var paymentIntent = stripePaymentService.createPaymentIntent(
            amountPence, agreementNumber, customerEmail);

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
}
