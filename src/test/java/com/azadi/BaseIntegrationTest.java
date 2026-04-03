package com.azadi;

import com.azadi.auth.LoginAttemptTracker;
import com.azadi.config.RateLimitFilter;
import com.google.cloud.datastore.Datastore;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    private static final int FIRESTORE_PORT = 8081;
    private static final int STRIPE_MOCK_PORT = 12111;
    private static final DateTimeFormatter DOB_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
                .withExposedPorts(STRIPE_MOCK_PORT)
                .waitingFor(Wait.forLogMessage(".*Listening for HTTP.*", 1));
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
        registry.add("stripe.api-base", () -> "http://"
                + stripeMock.getHost() + ":" + stripeMock.getMappedPort(STRIPE_MOCK_PORT));
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

    protected record TestIds(long customerId, long agreementId) {}

    protected TestIds createTestData(LocalDate dob, String postcode, String agreementNumber) {
        KeyFactory customerKeyFactory = datastore.newKeyFactory().setKind("Customer");
        Key customerKey = datastore.allocateId(customerKeyFactory.newKey());

        String customerId = "CUST-" + customerKey.getId();

        Entity customer = Entity.newBuilder(customerKey)
                .set("customerId", customerId)
                .set("fullName", "Test Customer")
                .set("email", "test-" + customerKey.getId() + "@example.com")
                .set("dob", Timestamp.ofTimeSecondsAndNanos(dob.atStartOfDay(ZoneOffset.UTC).toEpochSecond(), 0))
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
                .set("apr", "6.9")
                .set("originalTermMonths", 48)
                .set("contractMileage", 10000)
                .set("excessPricePerMilePence", 10L)
                .set("lastPaymentPence", 45000L)
                .set("lastPaymentDate", Timestamp.ofTimeSecondsAndNanos(
                        LocalDate.of(2026, 3, 1).atStartOfDay(ZoneOffset.UTC).toEpochSecond(), 0))
                .set("nextPaymentPence", 45000L)
                .set("nextPaymentDate", Timestamp.ofTimeSecondsAndNanos(
                        LocalDate.of(2026, 4, 1).atStartOfDay(ZoneOffset.UTC).toEpochSecond(), 0))
                .set("paymentsRemaining", 24)
                .set("finalPaymentDate", Timestamp.ofTimeSecondsAndNanos(
                        LocalDate.of(2028, 3, 1).atStartOfDay(ZoneOffset.UTC).toEpochSecond(), 0))
                .build();
        datastore.put(agreement);

        return new TestIds(customerKey.getId(), agreementKey.getId());
    }

    protected long createTestCustomer(LocalDate dob, String postcode, String agreementNumber) {
        return createTestData(dob, postcode, agreementNumber).customerId();
    }

    protected String loginAs(String agreementNumber, LocalDate dob, String postcode) {
        // Use a cookie-aware RestTemplate to handle session fixation transparently.
        var cookieManager = new java.net.CookieManager();
        java.net.CookieHandler.setDefault(cookieManager);
        try {
            var client = new org.springframework.http.client.SimpleClientHttpRequestFactory();
            var rt = new org.springframework.web.client.RestTemplate(client);

            String baseUrl = "http://localhost:" + port;

            // GET /login — picks up session + CSRF cookies
            ResponseEntity<String> loginPage = rt.getForEntity(baseUrl + "/login", String.class);
            String csrfToken = extractCsrfToken(loginPage.getBody());

            // POST credentials — CookieHandler auto-sends cookies, follows 302
            var formData = new LinkedMultiValueMap<String, String>();
            formData.add("username", agreementNumber);
            formData.add("password", dob.format(DOB_FORMATTER) + "|" + postcode);
            if (csrfToken != null) {
                formData.add("_csrf", csrfToken);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            rt.postForEntity(baseUrl + "/login", new HttpEntity<>(formData, headers), String.class);

            // Extract the authenticated session cookie from the cookie store
            return cookieManager.getCookieStore().getCookies().stream()
                    .filter(c -> c.getName().contains("__session") || c.getName().contains("SESSION"))
                    .map(c -> c.getName() + "=" + c.getValue())
                    .findFirst()
                    .orElse("");
        } finally {
            java.net.CookieHandler.setDefault(null);
        }
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
        return response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.contains("__session") || c.contains("SESSION"))
                .map(c -> c.split(";")[0])
                .findFirst()
                .orElse("");
    }

    protected String extractAllCookies(ResponseEntity<String> response) {
        List<String> cookies = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE);
        if (cookies.isEmpty()) {
            return "";
        }
        return cookies.stream()
                .map(c -> c.split(";")[0])
                .collect(Collectors.joining("; "));
    }

    protected String buildCookieHeader(ResponseEntity<String> response, String sessionCookie) {
        List<String> cookies = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE);
        if (cookies.isEmpty()) {
            return sessionCookie;
        }
        String responseCookies = cookies.stream()
                .map(c -> c.split(";")[0])
                .collect(Collectors.joining("; "));

        if (sessionCookie != null && !sessionCookie.isEmpty()
                && !responseCookies.contains(sessionCookie)) {
            return sessionCookie + "; " + responseCookies;
        }
        return responseCookies;
    }

    protected HttpHeaders authenticatedHeaders(String sessionCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, sessionCookie);
        return headers;
    }
}
