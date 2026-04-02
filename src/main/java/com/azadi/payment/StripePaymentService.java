package com.azadi.payment;

import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentService {

    private final StripeClient stripeClient;
    private final String webhookSecret;

    public StripePaymentService(StripeClient stripeClient,
                                @Value("${stripe.webhook-secret}") String webhookSecret) {
        this.stripeClient = stripeClient;
        this.webhookSecret = webhookSecret;
    }

    public PaymentIntent createPaymentIntent(long amountPence, String agreementNumber,
                                             String customerEmail) throws StripeException {
        var params = PaymentIntentCreateParams.builder()
            .setAmount(amountPence)
            .setCurrency("gbp")
            .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
            .putMetadata("agreementNumber", agreementNumber)
            .putMetadata("customerEmail", customerEmail)
            .build();

        return stripeClient.paymentIntents().create(params);
    }

    public PaymentIntent capturePaymentIntent(String paymentIntentId) throws StripeException {
        return stripeClient.paymentIntents().capture(
            paymentIntentId, PaymentIntentCaptureParams.builder().build());
    }

    public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
        return stripeClient.paymentIntents().cancel(paymentIntentId);
    }

    public Event constructWebhookEvent(String payload, String sigHeader)
            throws SignatureVerificationException {
        return Webhook.constructEvent(payload, sigHeader, webhookSecret);
    }
}
