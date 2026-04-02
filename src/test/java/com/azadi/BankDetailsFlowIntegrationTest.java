package com.azadi;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.StructuredQuery;
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
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BankDetailsFlowIntegrationTest extends BaseIntegrationTest {

    private static final String AGREEMENT_NUMBER = "AGR-BANK-001";
    private static final LocalDate DOB = LocalDate.of(1985, 4, 20);
    private static final String POSTCODE = "LS1 1BA";

    private String sessionCookie;

    @BeforeEach
    void setUpTestData() {
        createTestCustomer(DOB, POSTCODE, AGREEMENT_NUMBER);
        sessionCookie = loginAs(AGREEMENT_NUMBER, DOB, POSTCODE);
    }

    @Test
    @DisplayName("POST update bank details stores encrypted values, GET shows masked values")
    void updateAndRetrieveBankDetails() {
        // Arrange
        HttpHeaders headers = authenticatedHeaders(sessionCookie);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("sortCode", "12-34-56");
        formData.add("accountNumber", "12345678");
        formData.add("accountName", "John Doe");

        // Act - update bank details
        ResponseEntity<String> updateResponse = restTemplate.exchange(
                "/api/bank-details",
                HttpMethod.POST,
                new HttpEntity<>(formData, headers),
                String.class);

        // Assert - update should succeed
        assertThat(updateResponse.getStatusCode().value()).isIn(200, 302);

        // Verify data is encrypted in Datastore
        var query = Query.newEntityQueryBuilder()
                .setKind("BankDetails")
                .build();
        var results = datastore.run(query);
        if (results.hasNext()) {
            Entity stored = results.next();
            // Sort code and account number should be encrypted (not plain text)
            assertThat(stored.getString("sortCode")).isNotEqualTo("12-34-56");
            assertThat(stored.getString("accountNumber")).isNotEqualTo("12345678");
            // Account name is stored as-is
            assertThat(stored.getString("accountName")).isEqualTo("John Doe");
        }

        // Act - retrieve bank details (should show masked values)
        ResponseEntity<String> getResponse = restTemplate.exchange(
                "/api/bank-details",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        // Assert - GET should return masked values
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody()).contains("**-**-56");
        assertThat(getResponse.getBody()).contains("****5678");
        assertThat(getResponse.getBody()).contains("John Doe");
    }
}
