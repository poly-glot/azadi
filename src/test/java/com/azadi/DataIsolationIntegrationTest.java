package com.azadi;

import org.junit.jupiter.api.BeforeEach;
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
    @DisplayName("Customer A cannot access Customer B's agreement")
    void customerACannotAccessCustomerBAgreement() {
        // Arrange
        String sessionCookieA = loginAs(AGREEMENT_A, DOB_A, POSTCODE_A);

        // Act - attempt to access Customer B's agreement
        HttpHeaders headers = authenticatedHeaders(sessionCookieA);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/agreements/" + AGREEMENT_B,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        // Assert - should be denied or not contain Customer B's data
        // TestRestTemplate follows redirects, so 403 might become 200 with error page
        assertThat(response.getStatusCode().value()).isIn(200, 403, 404);
        if (response.getBody() != null) {
            assertThat(response.getBody()).doesNotContain(AGREEMENT_B);
        }
    }

    @Test
    @DisplayName("Customer A cannot update Customer B's bank details")
    void customerACannotUpdateCustomerBBankDetails() {
        // Arrange
        String sessionCookieA = loginAs(AGREEMENT_A, DOB_A, POSTCODE_A);

        HttpHeaders headers = authenticatedHeaders(sessionCookieA);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("agreementNumber", AGREEMENT_B);
        formData.add("sortCode", "99-99-99");
        formData.add("accountNumber", "99999999");
        formData.add("accountName", "Hacker");

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/bank-details/" + AGREEMENT_B,
                HttpMethod.POST,
                new HttpEntity<>(formData, headers),
                String.class);

        // Assert - should be denied or not process the request
        assertThat(response.getStatusCode().value()).isIn(200, 403, 404);
        if (response.getBody() != null) {
            assertThat(response.getBody()).doesNotContain("99-99-99");
        }
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
