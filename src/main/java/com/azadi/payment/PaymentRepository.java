package com.azadi.payment;

import com.google.cloud.spring.data.datastore.repository.DatastoreRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends DatastoreRepository<PaymentRecord, Long> {

    List<PaymentRecord> findByCustomerId(String customerId);

    Optional<PaymentRecord> findByStripePaymentIntentId(String stripePaymentIntentId);

    Optional<PaymentRecord> findByWebhookEventId(String webhookEventId);
}
