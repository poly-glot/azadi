package com.azadi.config;

import com.stripe.StripeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Bean
    public StripeClient stripeClient(@Value("${stripe.api-key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${") || "CHANGE_ME".equals(apiKey)) {
            throw new IllegalStateException(
                "Stripe API key is not configured. Set the STRIPE_API_KEY environment variable.");
        }
        return new StripeClient(apiKey);
    }
}
