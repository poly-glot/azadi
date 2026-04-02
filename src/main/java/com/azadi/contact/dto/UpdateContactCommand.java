package com.azadi.contact.dto;

import com.azadi.auth.Customer;

import java.util.Optional;

/**
 * Merges partial form submissions over existing customer state.
 * Each inline-edit form on the contact details page submits only the field being edited;
 * all other fields remain unchanged.
 */
public record UpdateContactCommand(
    String phone,
    String mobilePhone,
    String email,
    String addressLine1,
    String addressLine2,
    String city,
    String postcode
) {

    public static UpdateContactCommand mergeWith(Customer existing,
                                                  String homePhone,
                                                  String mobilePhone,
                                                  String email,
                                                  String houseName,
                                                  String postcode) {
        return new UpdateContactCommand(
            Optional.ofNullable(homePhone).filter(s -> !s.isBlank()).orElse(existing.getPhone()),
            Optional.ofNullable(mobilePhone).filter(s -> !s.isBlank()).orElse(existing.getMobilePhone()),
            Optional.ofNullable(email).filter(s -> !s.isBlank()).orElse(existing.getEmail()),
            Optional.ofNullable(houseName).filter(s -> !s.isBlank()).orElse(existing.getAddressLine1()),
            existing.getAddressLine2(),
            existing.getCity(),
            Optional.ofNullable(postcode).filter(s -> !s.isBlank()).orElse(existing.getPostcode())
        );
    }
}
