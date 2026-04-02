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

class AgreementFlowIntegrationTest extends BaseIntegrationTest {

    private static final String AGREEMENT_NUMBER = "AGR-FLOW-001";
    private static final LocalDate DOB = LocalDate.of(1988, 7, 14);
    private static final String POSTCODE = "M1 1AE";

    @BeforeEach
    void setUpTestData() {
        createTestCustomer(DOB, POSTCODE, AGREEMENT_NUMBER);
    }

    @Test
    @DisplayName("Login and access /my-account shows agreement data in response")
    void loginAndViewAgreementData() {
        // Arrange
        String sessionCookie = loginAs(AGREEMENT_NUMBER, DOB, POSTCODE);

        // Act
        HttpHeaders headers = authenticatedHeaders(sessionCookie);
        ResponseEntity<String> response = restTemplate.exchange(
                "/my-account", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains(AGREEMENT_NUMBER);
        assertThat(response.getBody()).contains("2024 Test Vehicle");
    }
}
