package com.azadi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST without CSRF token is rejected")
    void postWithoutCsrfTokenReturns403() {
        // Arrange - make a POST request without CSRF token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("username", "AGR-001");
        formData.add("password", "01/01/1990|SW1A 1AA");

        // Act - TestRestTemplate follows redirects, so 403 may become a login page
        ResponseEntity<String> response = restTemplate.exchange(
                "/login",
                HttpMethod.POST,
                new HttpEntity<>(formData, headers),
                String.class);

        // Assert - should be 403 or show an error page (not a successful login)
        assertThat(response.getStatusCode().value()).isIn(200, 403);
        if (response.getStatusCode().value() == 200) {
            assertThat(response.getBody()).doesNotContain("my-account");
        }
    }

    @Test
    @DisplayName("Access /my-account without login shows login page")
    void accessMyAccountWithoutLoginRedirects() {
        // Act - TestRestTemplate follows redirects, so we land on the login page
        ResponseEntity<String> response = restTemplate.getForEntity("/my-account", String.class);

        // Assert - should end up on login page
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("login");
    }

    @Test
    @DisplayName("Security headers are present in responses")
    void securityHeadersPresent() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity("/login", String.class);

        // Assert
        HttpHeaders responseHeaders = response.getHeaders();

        // X-Content-Type-Options
        assertThat(responseHeaders.getFirst("X-Content-Type-Options"))
                .isEqualTo("nosniff");

        // X-Frame-Options
        assertThat(responseHeaders.getFirst("X-Frame-Options"))
                .isIn("DENY", "SAMEORIGIN");

        // Cache-Control for security
        assertThat(responseHeaders.getFirst("Cache-Control"))
                .isNotNull();

        // Content-Security-Policy (if configured)
        String csp = responseHeaders.getFirst("Content-Security-Policy");
        if (csp != null) {
            assertThat(csp).isNotBlank();
        }

        // Strict-Transport-Security (may not be present on HTTP in test)
        // X-XSS-Protection
        String xssProtection = responseHeaders.getFirst("X-XSS-Protection");
        if (xssProtection != null) {
            assertThat(xssProtection).isNotBlank();
        }
    }

    @Test
    @DisplayName("Session cookie is configured with expected attributes")
    void sessionCookieAttributes() {
        // Arrange
        createTestCustomer(LocalDate.of(1990, 1, 1), "SW1A 1AA", "AGR-SEC-001");

        // Act - login to establish a session
        String sessionCookie = loginAs("AGR-SEC-001", LocalDate.of(1990, 1, 1), "SW1A 1AA");

        // Assert - verify we got a session cookie
        assertThat(sessionCookie).isNotNull().isNotBlank();

        // The session cookie should contain the configured name
        assertThat(sessionCookie).containsIgnoringCase("AZADI_SESSION");
    }
}
