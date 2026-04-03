package com.azadi.config;

import com.stripe.Stripe;
import com.stripe.StripeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Bean
    public StripeClient stripeClient(@Value("${stripe.api-key}") String apiKey,
                                     @Value("${stripe.api-base:}") String apiBase) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${") || "CHANGE_ME".equals(apiKey)) {
            throw new IllegalStateException(
                "Stripe API key is not configured. Set the STRIPE_API_KEY environment variable.");
        }
        Stripe.apiKey = apiKey;
        if (apiBase != null && !apiBase.isBlank()) {
            Stripe.overrideApiBase(apiBase);
        }
        return new StripeClient(apiKey);
    }
}
