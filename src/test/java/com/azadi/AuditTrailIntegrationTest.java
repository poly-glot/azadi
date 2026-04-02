package com.azadi;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditTrailIntegrationTest extends BaseIntegrationTest {

    private static final String AGREEMENT_NUMBER = "AGR-AUDIT-001";
    private static final LocalDate DOB = LocalDate.of(1991, 8, 22);
    private static final String POSTCODE = "EH1 1YZ";

    @BeforeEach
    void setUpTestData() {
        createTestCustomer(DOB, POSTCODE, AGREEMENT_NUMBER);
    }

    @Test
    @DisplayName("Successful login does not create audit event (login auditing not implemented)")
    void loginDoesNotCreateAuditEvent() {
        // Act
        loginAs(AGREEMENT_NUMBER, DOB, POSTCODE);

        // Assert - login auditing is not yet implemented
        List<Entity> auditEvents = queryAuditEvents();
        assertThat(auditEvents).isEmpty();
    }

    @Test
    @DisplayName("Viewing account page does not create audit event (not implemented)")
    void viewAgreementDoesNotCreateAuditEvent() {
        // Arrange
        String sessionCookie = loginAs(AGREEMENT_NUMBER, DOB, POSTCODE);

        // Act - access the account page
        HttpHeaders headers = authenticatedHeaders(sessionCookie);
        restTemplate.exchange("/my-account", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        // Assert - agreement view auditing is not yet implemented
        List<Entity> auditEvents = queryAuditEvents();
        boolean hasViewEvent = auditEvents.stream()
                .anyMatch(e -> "AGREEMENT_VIEWED".equals(e.getString("eventType")));
        assertThat(hasViewEvent).isFalse();
    }

    @Test
    @DisplayName("Contact details update creates CONTACT_DETAILS_UPDATED audit event")
    void contactUpdateCreatesAuditEvent() {
        // Arrange
        String sessionCookie = loginAs(AGREEMENT_NUMBER, DOB, POSTCODE);

        HttpHeaders headers = authenticatedHeaders(sessionCookie);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("phone", "07000000001");
        formData.add("email", "audit-test@example.com");
        formData.add("addressLine1", "1 Test Lane");
        formData.add("city", "London");
        formData.add("postcode", "EH1 1YZ");

        // Act
        restTemplate.exchange(
                "/my-contact-details",
                HttpMethod.POST,
                new HttpEntity<>(formData, headers),
                String.class);

        // Assert
        List<Entity> auditEvents = queryAuditEvents();
        boolean hasContactEvent = auditEvents.stream()
                .anyMatch(e -> "CONTACT_DETAILS_UPDATED".equals(e.getString("eventType")));
        assertThat(hasContactEvent)
                .as("Should have a CONTACT_DETAILS_UPDATED audit event")
                .isTrue();
    }

    @Test
    @DisplayName("Audit events contain required fields")
    void auditEventsContainRequiredFields() {
        // Arrange - trigger an audit event via contact update
        String sessionCookie = loginAs(AGREEMENT_NUMBER, DOB, POSTCODE);

        HttpHeaders headers = authenticatedHeaders(sessionCookie);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("phone", "07000000002");
        formData.add("email", "audit-fields@example.com");
        formData.add("addressLine1", "2 Test Lane");
        formData.add("city", "London");
        formData.add("postcode", "EH1 1YZ");

        restTemplate.exchange(
                "/my-contact-details",
                HttpMethod.POST,
                new HttpEntity<>(formData, headers),
                String.class);

        // Assert
        List<Entity> auditEvents = queryAuditEvents();
        assertThat(auditEvents).isNotEmpty();

        Entity event = auditEvents.getFirst();
        assertThat(event.contains("eventType")).isTrue();
        assertThat(event.contains("customerId")).isTrue();
        assertThat(event.contains("timestamp")).isTrue();
    }

    private List<Entity> queryAuditEvents() {
        var query = Query.newEntityQueryBuilder()
                .setKind("AuditEvent")
                .build();
        QueryResults<Entity> results = datastore.run(query);

        List<Entity> events = new ArrayList<>();
        while (results.hasNext()) {
            events.add(results.next());
        }
        return events;
    }
}
