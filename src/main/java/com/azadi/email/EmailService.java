package com.azadi.email;

import com.azadi.auth.CustomerRepository;
import com.azadi.email.templates.BankDetailsUpdatedTemplate;
import com.azadi.email.templates.LoginAlertTemplate;
import com.azadi.email.templates.PaymentConfirmationTemplate;
import com.azadi.email.templates.PaymentDateChangedTemplate;
import com.azadi.email.templates.SettlementFigureTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final HttpClient httpClient;
    private final JsonMapper objectMapper;
    private final String apiKey;
    private final String fromEmail;
    private final CustomerRepository customerRepository;

    public EmailService(@Value("${resend.api-key}") String apiKey,
                        @Value("${resend.from-email}") String fromEmail,
                        CustomerRepository customerRepository,
                        JsonMapper objectMapper,
                        HttpClient httpClient) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.customerRepository = customerRepository;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public void sendEmail(String to, String subject, String html) {
        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of(
                "from", fromEmail,
                "to", List.of(to),
                "subject", subject,
                "html", html
            ));
        } catch (JacksonException e) {
            LOG.error("Failed to serialize email payload for {}", to, e);
            return;
        }

        var request = HttpRequest.newBuilder()
            .uri(URI.create(RESEND_API_URL))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                if (response.statusCode() >= 400) {
                    LOG.error("Failed to send email to {}: {} - {}", to, response.statusCode(), response.body());
                } else {
                    LOG.info("Email sent successfully to {}", to);
                }
            })
            .exceptionally(throwable -> {
                LOG.error("Error sending email to {}", to, throwable);
                return null;
            });
    }

    public void sendPaymentConfirmation(String customerId, long amountPence) {
        resolveEmailAndSend(customerId, "Payment Confirmation - Azadi Finance",
            PaymentConfirmationTemplate.build(amountPence));
    }

    public void sendSettlementFigure(String customerId, long amountPence, String validUntil) {
        resolveEmailAndSend(customerId, "Your Settlement Figure - Azadi Finance",
            SettlementFigureTemplate.build(amountPence, validUntil));
    }

    public void sendBankDetailsUpdated(String customerId) {
        resolveEmailAndSend(customerId, "Bank Details Updated - Azadi Finance",
            BankDetailsUpdatedTemplate.build());
    }

    public void sendPaymentDateChanged(String customerId, String newDate) {
        resolveEmailAndSend(customerId, "Payment Date Changed - Azadi Finance",
            PaymentDateChangedTemplate.build(newDate));
    }

    public void sendLoginAlert(String customerId, String ipAddress) {
        resolveEmailAndSend(customerId, "New Login Detected - Azadi Finance",
            LoginAlertTemplate.build(ipAddress));
    }

    private void resolveEmailAndSend(String customerId, String subject, String html) {
        customerRepository.findByCustomerId(customerId).ifPresent(customer -> {
            if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
                sendEmail(customer.getEmail(), subject, html);
            }
        });
    }
}
