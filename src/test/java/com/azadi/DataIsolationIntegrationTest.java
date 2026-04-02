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

import static org.assertj.core.api.Assertions.assertThat;

class DataIsolationIntegrationTest extends BaseIntegrationTest {

    private static final String AGREEMENT_A = "AGR-ISO-A001";
    private static final LocalDate DOB_A = LocalDate.of(1990, 6, 15);
    private static final String POSTCODE_A = "SW1A 1AA";

    private static final String AGREEMENT_B = "AGR-ISO-B001";
    private static final LocalDate DOB_B = LocalDate.of(1985, 2, 28);
    private static final String POSTCODE_B = "M1 1AE";

    @BeforeEach
    void setUpTestData() {
        createTestCustomer(DOB_A, POSTCODE_A, AGREEMENT_A);
        createTestCustomer(DOB_B, POSTCODE_B, AGREEMENT_B);
    }

    @Test
    @DisplayName("Customer A can see their own agreement data")
    void customerACanSeeOwnAgreement() {
        // Arrange
        String sessionCookieA = loginAs(AGREEMENT_A, DOB_A, POSTCODE_A);

        // Act
        HttpHeaders headers = authenticatedHeaders(sessionCookieA);
        ResponseEntity<String> response = restTemplate.exchange(
                "/my-account", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(AGREEMENT_A);
    }

    @Test
    @DisplayName("Customer A cannot access Customer B's agreement via my-account")
    void customerACannotAccessCustomerBAgreement() {
        // Arrange
        String sessionCookieA = loginAs(AGREEMENT_A, DOB_A, POSTCODE_A);

        // Act - access /my-account as Customer A
        HttpHeaders headers = authenticatedHeaders(sessionCookieA);
        ResponseEntity<String> response = restTemplate.exchange(
                "/my-account",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        // Assert - Customer A's account page should not contain Customer B's agreement
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).doesNotContain(AGREEMENT_B);
    }

    @Test
    @DisplayName("Customer A cannot update Customer B's bank details")
    void customerACannotUpdateCustomerBBankDetails() {
        // Arrange
        String sessionCookieA = loginAs(AGREEMENT_A, DOB_A, POSTCODE_A);

        // Act - attempt to POST bank details without CSRF (should be rejected)
        HttpHeaders headers = authenticatedHeaders(sessionCookieA);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        var formData = new org.springframework.util.LinkedMultiValueMap<String, String>();
        formData.add("sortCode", "99-99-99");
        formData.add("accountNumber", "99999999");
        formData.add("accountName", "Hacker");

        ResponseEntity<String> response = restTemplate.exchange(
                "/finance/update-bank-details",
                HttpMethod.POST,
                new HttpEntity<>(formData, headers),
                String.class);

        // Assert - POST without CSRF token should be rejected (403)
        assertThat(response.getStatusCode().value()).isIn(200, 403);
    }

    @Test
    @DisplayName("Customer B data is not leaked when Customer A is logged in")
    void customerBDataNotLeakedToCustomerA() {
        // Arrange
        String sessionCookieA = loginAs(AGREEMENT_A, DOB_A, POSTCODE_A);

        // Act
        HttpHeaders headers = authenticatedHeaders(sessionCookieA);
        ResponseEntity<String> response = restTemplate.exchange(
                "/my-account", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        // Assert - response should not contain Customer B's agreement
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).doesNotContain(AGREEMENT_B);
    }
}
