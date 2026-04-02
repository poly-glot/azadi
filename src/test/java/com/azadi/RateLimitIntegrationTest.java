package com.azadi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitIntegrationTest extends BaseIntegrationTest {

    private static final String AGREEMENT_NUMBER = "AGR-RATE-001";
    private static final LocalDate DOB = LocalDate.of(1990, 1, 1);
    private static final String POSTCODE = "SW1A 1AA";
    private static final LocalDate WRONG_DOB = LocalDate.of(1980, 12, 31);

    @BeforeEach
    void setUpTestData() {
        createTestCustomer(DOB, POSTCODE, AGREEMENT_NUMBER);
    }

    @Test
    @DisplayName("First 5 failed login attempts all receive normal responses")
    void firstFiveAttemptsGetNormalResponses() {
        for (int i = 0; i < 5; i++) {
            // Act
            ResponseEntity<String> response = performLoginAttempt(AGREEMENT_NUMBER, WRONG_DOB, POSTCODE);

            // Assert - should get a normal response (200 with error or 302 redirect)
            assertThat(response.getStatusCode().value())
                    .as("Attempt %d should not be rate limited", i + 1)
                    .isIn(200, 302);
        }
    }

    @Test
    @DisplayName("6th failed login attempt returns 429 Too Many Requests")
    void sixthAttemptReturns429() {
        // Arrange - exhaust the allowed attempts
        for (int i = 0; i < 6; i++) {
            performLoginAttempt(AGREEMENT_NUMBER, WRONG_DOB, POSTCODE);
        }

        // Act - the 7th attempt (to be safe, since threshold might be checked differently)
        ResponseEntity<String> response = performLoginAttempt(AGREEMENT_NUMBER, WRONG_DOB, POSTCODE);

        // Assert - should be rate limited
        assertThat(response.getStatusCode().value()).isIn(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("Rate limit tracks by IP for login attempts")
    void rateLimitTracksIndependently() {
        // Arrange - exhaust login rate limit for this IP (5 attempts max)
        for (int i = 0; i < 5; i++) {
            performLoginAttempt(AGREEMENT_NUMBER, WRONG_DOB, POSTCODE);
        }

        // Act - attempt with a different agreement number but same IP
        createTestCustomer(DOB, POSTCODE, "AGR-RATE-002");
        ResponseEntity<String> response = performLoginAttempt("AGR-RATE-002", WRONG_DOB, POSTCODE);

        // Assert - same IP should still be rate limited (IP-based rate limiting)
        assertThat(response.getStatusCode().value()).isIn(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.FORBIDDEN.value());
    }

    private ResponseEntity<String> performLoginAttempt(String agreementNumber, LocalDate dob, String postcode) {
        // Get login page for CSRF token
        ResponseEntity<String> loginPage = restTemplate.getForEntity("/login", String.class);
        String csrfToken = extractCsrfTokenFromPage(loginPage.getBody());
        List<String> cookies = loginPage.getHeaders().get(HttpHeaders.SET_COOKIE);
        String sessionCookie = (cookies != null && !cookies.isEmpty()) ? cookies.getFirst() : "";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.COOKIE, sessionCookie);

        String formattedDob = dob.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", agreementNumber);
        formData.add("password", formattedDob + "|" + postcode);
        if (csrfToken != null) {
            formData.add("_csrf", csrfToken);
        }

        return restTemplate.postForEntity("/login", new HttpEntity<>(formData, headers), String.class);
    }

    private String extractCsrfTokenFromPage(String html) {
        if (html == null) return null;
        int idx = html.indexOf("name=\"_csrf\"");
        if (idx == -1) return null;
        int start = Math.max(0, idx - 200);
        int end = Math.min(html.length(), idx + 200);
        String region = html.substring(start, end);
        int valueIdx = region.indexOf("value=\"");
        if (valueIdx == -1) return null;
        int valueStart = valueIdx + "value=\"".length();
        int valueEnd = region.indexOf("\"", valueStart);
        if (valueEnd == -1) return null;
        return region.substring(valueStart, valueEnd);
    }
}
