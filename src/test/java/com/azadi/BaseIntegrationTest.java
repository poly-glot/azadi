package com.azadi;

import com.azadi.auth.LoginAttemptTracker;
import com.azadi.config.RateLimitFilter;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.google.cloud.Timestamp;

import java.time.LocalDate;
import java.time.ZoneOffset;
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
        // Clean known kinds between tests to ensure isolation
        cleanKind("Customer");
        cleanKind("Agreement");
        cleanKind("Payment");
        cleanKind("BankDetails");
        cleanKind("AuditEvent");
        cleanKind("Settlement");

        // Reset in-memory state to prevent cross-test contamination
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

    /**
     * Inserts a test customer entity into the Datastore emulator.
     *
     * @param dob             date of birth
     * @param postcode        UK postcode
     * @param agreementNumber the finance agreement number
     * @return the generated customer ID
     */
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

        // Also create a corresponding agreement entity
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

    /**
     * Performs a login via the TestRestTemplate and returns the session cookie
     * for subsequent authenticated requests.
     *
     * @param agreementNumber the agreement number to log in with
     * @param dob             date of birth
     * @param postcode        postcode
     * @return the Set-Cookie header value containing the session ID
     */
    protected String loginAs(String agreementNumber, LocalDate dob, String postcode) {
        try {
            String baseUrl = "http://localhost:" + port;

            // 1. GET /login to obtain CSRF token and session cookie
            var loginUrl = new java.net.URL(baseUrl + "/login");
            var loginConn = (java.net.HttpURLConnection) loginUrl.openConnection();
            loginConn.setInstanceFollowRedirects(false);
            loginConn.connect();
            String body = new String(loginConn.getInputStream().readAllBytes());
            String csrfToken = extractCsrfToken(body);

            // Extract all cookies from the GET response
            String allCookies = "";
            var cookieHeaders = loginConn.getHeaderFields().get("Set-Cookie");
            if (cookieHeaders != null) {
                allCookies = String.join("; ",
                    cookieHeaders.stream().map(c -> c.split(";")[0]).toList());
            }
            loginConn.disconnect();

            // 2. POST /login with credentials (no redirect following)
            String formattedDob = dob.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String formBody = "username=" + java.net.URLEncoder.encode(agreementNumber, "UTF-8")
                    + "&password=" + java.net.URLEncoder.encode(formattedDob + "|" + postcode, "UTF-8")
                    + "&_csrf=" + java.net.URLEncoder.encode(csrfToken != null ? csrfToken : "", "UTF-8");

            var postUrl = new java.net.URL(baseUrl + "/login");
            var postConn = (java.net.HttpURLConnection) postUrl.openConnection();
            postConn.setRequestMethod("POST");
            postConn.setInstanceFollowRedirects(false);
            postConn.setDoOutput(true);
            postConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            postConn.setRequestProperty("Cookie", allCookies);
            postConn.getOutputStream().write(formBody.getBytes());
            postConn.getOutputStream().flush();

            int status = postConn.getResponseCode();

            // Extract session cookie from 302 response (session fixation migration creates new session)
            var postCookies = postConn.getHeaderFields().get("Set-Cookie");
            if (postCookies != null) {
                for (String cookie : postCookies) {
                    if (cookie.contains("AZADI_SESSION") || cookie.contains("SESSION")) {
                        return cookie.split(";")[0];
                    }
                }
            }
            postConn.disconnect();

            // Fall back to initial cookies
            return allCookies;
        } catch (Exception e) {
            throw new RuntimeException("Login failed", e);
        }
    }

    /**
     * Extracts a CSRF token from an HTML page body. Looks for a hidden input field
     * named _csrf.
     */
    private String extractCsrfToken(String html) {
        if (html == null) {
            return null;
        }
        // Look for <input type="hidden" name="_csrf" value="..." />
        int nameIndex = html.indexOf("name=\"_csrf\"");
        if (nameIndex == -1) {
            return null;
        }
        // Search backward and forward from nameIndex for value attribute
        int searchStart = Math.max(0, nameIndex - 200);
        int searchEnd = Math.min(html.length(), nameIndex + 200);
        String region = html.substring(searchStart, searchEnd);

        int valueIndex = region.indexOf("value=\"");
        if (valueIndex == -1) {
            return null;
        }
        int valueStart = valueIndex + "value=\"".length();
        int valueEnd = region.indexOf("\"", valueStart);
        if (valueEnd == -1) {
            return null;
        }
        return region.substring(valueStart, valueEnd);
    }

    /**
     * Creates an authenticated HttpHeaders instance with the given session cookie.
     */
    protected HttpHeaders authenticatedHeaders(String sessionCookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, sessionCookie);
        return headers;
    }
}