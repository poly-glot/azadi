package com.azadi;

import com.azadi.auth.LoginAttemptTracker;
import com.azadi.config.RateLimitFilter;
import com.google.cloud.datastore.Datastore;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    private static final int FIRESTORE_PORT = 8081;
    private static final int STRIPE_MOCK_PORT = 12111;

    static final GenericContainer<?> firestoreEmulator;
    static final GenericContainer<?> stripeMock;

    static {
        firestoreEmulator = new GenericContainer<>(
                "gcr.io/google.com/cloudsdktool/google-cloud-cli:emulators")
                .withCommand(
                        "gcloud", "emulators", "firestore", "start",
                        "--host-port=0.0.0.0:8081",
                        "--database-mode=datastore-mode",
                        "--project=test-project")
                .withExposedPorts(FIRESTORE_PORT)
                .waitingFor(Wait.forLogMessage(".*Dev App Server is now running.*", 1));
        firestoreEmulator.start();

        stripeMock = new GenericContainer<>("stripe/stripe-mock:latest")
                .withExposedPorts(STRIPE_MOCK_PORT);
        stripeMock.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String firestoreHost = firestoreEmulator.getHost() + ":"
                + firestoreEmulator.getMappedPort(FIRESTORE_PORT);

        registry.add("FIRESTORE_EMULATOR_HOST", () -> firestoreHost);
        registry.add("spring.cloud.gcp.datastore.emulator.enabled", () -> "true");
        registry.add("spring.cloud.gcp.project-id", () -> "test-project");
        registry.add("stripe.api-key", () -> "sk_test_mock");
        registry.add("stripe.webhook-secret", () -> "whsec_test_mock");
    }

    @LocalServerPort
    private int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected Datastore datastore;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Autowired
    private LoginAttemptTracker loginAttemptTracker;

    @BeforeEach
    void clearDatastore() {
        cleanKind("Customer");
        cleanKind("Agreement");
        cleanKind("Payment");
        cleanKind("BankDetails");
        cleanKind("AuditEvent");
        cleanKind("Settlement");

        rateLimitFilter.clearAll();
        loginAttemptTracker.clearAll();
    }

    private void cleanKind(String kind) {
        var query = com.google.cloud.datastore.Query.newKeyQueryBuilder()
                .setKind(kind)
                .build();
        var results = datastore.run(query);
        while (results.hasNext()) {
            datastore.delete(results.next());
        }
    }

    protected long createTestCustomer(LocalDate dob, String postcode, String agreementNumber) {
        KeyFactory customerKeyFactory = datastore.newKeyFactory().setKind("Customer");
        Key customerKey = datastore.allocateId(customerKeyFactory.newKey());

        String customerId = "CUST-" + customerKey.getId();

        Entity customer = Entity.newBuilder(customerKey)
                .set("customerId", customerId)
                .set("fullName", "Test Customer")
                .set("email", "test-" + customerKey.getId() + "@example.com")
                .set("dob", Timestamp.of(java.sql.Timestamp.valueOf(dob.atStartOfDay())))
                .set("postcode", postcode.toUpperCase().trim())
                .set("phone", "07000000000")
                .set("addressLine1", "1 Test Street")
                .set("city", "London")
                .build();
        datastore.put(customer);

        KeyFactory agreementKeyFactory = datastore.newKeyFactory().setKind("Agreement");
        Key agreementKey = datastore.allocateId(agreementKeyFactory.newKey());

        Entity agreement = Entity.newBuilder(agreementKey)
                .set("agreementNumber", agreementNumber)
                .set("customerId", customerId)
                .set("type", "PCP")
                .set("vehicleModel", "2024 Test Vehicle")
                .set("registration", "AB24 TST")
                .set("balancePence", 1200000L)
                .set("apr", 6.9)
                .set("originalTermMonths", 48)
                .set("contractMileage", 10000)
                .set("excessPricePerMilePence", 10L)
                .set("lastPaymentPence", 45000L)
                .set("lastPaymentDate", Timestamp.of(java.sql.Timestamp.valueOf(
                        LocalDate.of(2026, 3, 1).atStartOfDay())))
                .set("nextPaymentPence", 45000L)
                .set("nextPaymentDate", Timestamp.of(java.sql.Timestamp.valueOf(
                        LocalDate.of(2026, 4, 1).atStartOfDay())))
                .set("paymentsRemaining", 24)
                .set("finalPaymentDate", Timestamp.of(java.sql.Timestamp.valueOf(
                        LocalDate.of(2028, 3, 1).atStartOfDay())))
                .build();
        datastore.put(agreement);

        return customerKey.getId();
    }

    protected String loginAs(String agreementNumber, LocalDate dob, String postcode) {
        ResponseEntity<String> loginPage = restTemplate.getForEntity("/login", String.class);
        String csrfToken = extractCsrfToken(loginPage.getBody());
        String sessionCookie = extractSessionCookie(loginPage);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.COOKIE, sessionCookie);

        String formattedDob = dob.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("username", agreementNumber);
        formData.add("password", formattedDob + "|" + postcode);
        if (csrfToken != null) {
            formData.add("_csrf", csrfToken);
        }

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/login", new HttpEntity<>(formData, headers), String.class);

        List<String> postCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (postCookies != null) {
            for (String cookie : postCookies) {
                if (cookie.contains("AZADI_SESSION") || cookie.contains("SESSION")) {
                    return cookie.split(";")[0];
                }
            }
        }

        return sessionCookie;
    }

    protected String extractCsrfToken(String html) {
        if (html == null) {
            return null;
        }
        var el = Jsoup.parse(html).selectFirst("input[name=_csrf]");
        return el != null ? el.attr("value") : null;
    }

    protected String extractCsrf(ResponseEntity<String> response) {
        return extractCsrfToken(response.getBody());
    }

    protected String extractSessionCookie(ResponseEntity<String> response) {
        var cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        return (cookies != null && !cookies.isEmpty()) ? cookies.getFirst().split(";")[0] : "";
    }

    protected String buildCookieHeader(ResponseEntity<String> response, String sessionCookie) {
        var cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies != null && !cookies.isEmpty()) {
            var newSession = cookies.stream()
                .filter(c -> c.contains("AZADI_SESSION") || c.contains("SESSION"))
                .map(c -> c.split(";")[0])
                .findFirst()
                .orElse(sessionCookie);
            return newSession;
        }
        return sessionCookie;
    }

    protected HttpHeaders authenticatedHeaders(String sessionCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, sessionCookie);
        return headers;
    }
}
