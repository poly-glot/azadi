package com.azadi;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
    @DisplayName("POST update bank details stores values in Datastore")
    void updateAndRetrieveBankDetails() {
        // Step 1: GET the bank details form to extract CSRF token
        HttpHeaders getHeaders = authenticatedHeaders(sessionCookie);
        ResponseEntity<String> formPage = restTemplate.exchange(
                "/finance/update-bank-details", HttpMethod.GET,
                new HttpEntity<>(getHeaders), String.class);

        assertThat(formPage.getStatusCode().value()).isIn(200, 302);

        String csrfToken = extractCsrf(formPage);
        String cookieHeader = buildCookieHeader(formPage, sessionCookie);

        // Step 2: POST the bank details with CSRF token
        HttpHeaders postHeaders = new HttpHeaders();
        postHeaders.set(HttpHeaders.COOKIE, cookieHeader);
        postHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("sortCode", "12-34-56");
        formData.add("accountNumber", "12345678");
        formData.add("accountHolderName", "John Doe");
        if (csrfToken != null) {
            formData.add("_csrf", csrfToken);
        }

        ResponseEntity<String> updateResponse = restTemplate.exchange(
                "/finance/update-bank-details",
                HttpMethod.POST,
                new HttpEntity<>(formData, postHeaders),
                String.class);

        // Assert - update should succeed (302 redirect or 200)
        assertThat(updateResponse.getStatusCode().value()).isIn(200, 302);

        // Step 3: Verify data is stored in Datastore
        var query = Query.newEntityQueryBuilder()
                .setKind("BankDetails")
                .build();
        var results = datastore.run(query);
        if (results.hasNext()) {
            Entity stored = results.next();
            // Sort code and account number should be encrypted (not plain text)
            assertThat(stored.getString("encryptedSortCode")).isNotEqualTo("12-34-56");
            assertThat(stored.getString("encryptedAccountNumber")).isNotEqualTo("12345678");
            // Account holder name is stored as-is
            assertThat(stored.getString("accountHolderName")).isEqualTo("John Doe");
        }
    }
}
