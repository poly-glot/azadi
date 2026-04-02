package com.azadi.contact;

import com.azadi.auth.Customer;
import com.azadi.contact.dto.UpdateContactCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateContactCommandTest {

    @Test
    @DisplayName("Non-null submitted fields override existing values")
    void mergeWith_overridesNonNullFields() {
        var existing = buildCustomer();

        var command = UpdateContactCommand.mergeWith(existing,
            "02012345678", null, "new@example.com", null, null);

        assertThat(command.phone()).isEqualTo("02012345678");
        assertThat(command.mobilePhone()).isEqualTo("07999111222");
        assertThat(command.email()).isEqualTo("new@example.com");
        assertThat(command.addressLine1()).isEqualTo("1 Old Street");
        assertThat(command.postcode()).isEqualTo("SW1A 1AA");
    }

    @Test
    @DisplayName("Null fields fall back to existing customer values")
    void mergeWith_fallsBackToExisting() {
        var existing = buildCustomer();

        var command = UpdateContactCommand.mergeWith(existing,
            null, null, null, null, null);

        assertThat(command.phone()).isEqualTo("02000000000");
        assertThat(command.mobilePhone()).isEqualTo("07999111222");
        assertThat(command.email()).isEqualTo("old@example.com");
        assertThat(command.addressLine1()).isEqualTo("1 Old Street");
    }

    @Test
    @DisplayName("Blank strings are treated as absent and fall back to existing")
    void mergeWith_blankTreatedAsAbsent() {
        var existing = buildCustomer();

        var command = UpdateContactCommand.mergeWith(existing,
            "  ", null, "", null, null);

        assertThat(command.phone()).isEqualTo("02000000000");
        assertThat(command.email()).isEqualTo("old@example.com");
    }

    @Test
    @DisplayName("Mobile phone submitted alone updates only mobile")
    void mergeWith_mobilePhoneOnly() {
        var existing = buildCustomer();

        var command = UpdateContactCommand.mergeWith(existing,
            null, "07111222333", null, null, null);

        assertThat(command.phone()).isEqualTo("02000000000");
        assertThat(command.mobilePhone()).isEqualTo("07111222333");
    }

    @Test
    @DisplayName("Address and postcode submitted together update both")
    void mergeWith_addressAndPostcode() {
        var existing = buildCustomer();

        var command = UpdateContactCommand.mergeWith(existing,
            null, null, null, "42 New Road", "EC1A 1BB");

        assertThat(command.addressLine1()).isEqualTo("42 New Road");
        assertThat(command.postcode()).isEqualTo("EC1A 1BB");
        assertThat(command.city()).isEqualTo("London");
    }

    private Customer buildCustomer() {
        var c = new Customer();
        c.setPhone("02000000000");
        c.setMobilePhone("07999111222");
        c.setEmail("old@example.com");
        c.setAddressLine1("1 Old Street");
        c.setAddressLine2("Flat 2");
        c.setCity("London");
        c.setPostcode("SW1A 1AA");
        return c;
    }
}
