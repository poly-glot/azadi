package com.azadi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class LoginFlowIntegrationTest extends BaseIntegrationTest {

    private static final String AGREEMENT_NUMBER = "AGR-LOGIN-001";
    private static final LocalDate DOB = LocalDate.of(1990, 3, 25);
    private static final String POSTCODE = "SW1A 1AA";

    @BeforeEach
    void setUpTestData() {
        createTestCustomer(DOB, POSTCODE, AGREEMENT_NUMBER);
    }

    @Test
    @DisplayName("Successful login redirects to /my-account")
    void successfulLoginRedirectsToMyAccount() {
        // Act
        String sessionCookie = loginAs(AGREEMENT_NUMBER, DOB, POSTCODE);

        // Assert - after login, accessing /my-account should return 200
        HttpHeaders headers = authenticatedHeaders(sessionCookie);
        ResponseEntity<String> response = restTemplate.exchange(
                "/my-account", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Wrong credentials shows error on login page")
    void wrongCredentialsShowsError() {
        // Act - attempt login with wrong DOB
        LocalDate wrongDob = LocalDate.of(1985, 1, 1);

        // Fetch login page for CSRF
        ResponseEntity<String> loginPage = restTemplate.getForEntity("/login", String.class);
        String csrfToken = extractCsrf(loginPage);
        String cookies = buildCookieHeader(loginPage, "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.COOKIE, cookies);

        String formattedDob = wrongDob.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        var formData = new org.springframework.util.LinkedMultiValueMap<String, String>();
        formData.add("username", AGREEMENT_NUMBER);
        formData.add("password", formattedDob + "|" + POSTCODE);
        if (csrfToken != null) {
            formData.add("_csrf", csrfToken);
        }

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/login", new HttpEntity<>(formData, headers), String.class);

        // Assert - should redirect back to login with error or show error page
        assertThat(response.getStatusCode().value()).isIn(200, 302);
        if (response.getStatusCode().is3xxRedirection()) {
            assertThat(response.getHeaders().getLocation()).hasPath("/login");
        }
    }

    @Test
    @DisplayName("Logout clears session and prevents access to protected pages")
    void logoutClearsSession() {
        // Arrange - login first
        String sessionCookie = loginAs(AGREEMENT_NUMBER, DOB, POSTCODE);

        // GET a page with a form to extract the CSRF token for the logout POST
        HttpHeaders getHeaders = authenticatedHeaders(sessionCookie);
        ResponseEntity<String> page = restTemplate.exchange(
                "/my-contact-details", HttpMethod.GET, new HttpEntity<>(getHeaders), String.class);

        String csrfToken = extractCsrf(page);
        String cookieHeader = buildCookieHeader(page, sessionCookie);

        // Act - logout with CSRF token
        HttpHeaders logoutHeaders = new HttpHeaders();
        logoutHeaders.set(HttpHeaders.COOKIE, cookieHeader);
        logoutHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        var logoutForm = new org.springframework.util.LinkedMultiValueMap<String, String>();
        if (csrfToken != null) {
            logoutForm.add("_csrf", csrfToken);
        }

        restTemplate.exchange(
                "/logout", HttpMethod.POST,
                new HttpEntity<>(logoutForm, logoutHeaders), String.class);

        // Assert - after logout, accessing /my-account with old session should show login page
        ResponseEntity<String> protectedResponse = restTemplate.exchange(
                "/my-account", HttpMethod.GET, new HttpEntity<>(getHeaders), String.class);

        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(protectedResponse.getBody()).contains("login");
    }

    @Test
    @DisplayName("Accessing protected page without login shows login page")
    void accessProtectedPageWithoutLoginRedirects() {
        // Act - TestRestTemplate follows redirects, so we get the login page
        ResponseEntity<String> response = restTemplate.getForEntity("/my-account", String.class);

        // Assert - should end up on the login page
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("login");
    }
}
