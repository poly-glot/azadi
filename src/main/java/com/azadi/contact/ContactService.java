package com.azadi.contact;

import com.azadi.audit.AuditService;
import com.azadi.auth.Customer;
import com.azadi.auth.CustomerRepository;
import com.azadi.contact.dto.UpdateContactCommand;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class ContactService {

    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    public ContactService(CustomerRepository customerRepository,
                          AuditService auditService) {
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    public Customer getCustomer(String customerId) {
        return customerRepository.findByCustomerId(customerId)
            .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));
    }

    public void updateContactDetails(String customerId, UpdateContactCommand command, String ipAddress) {
        var customer = getCustomer(customerId);
        customer.setPhone(command.phone());
        customer.setMobilePhone(command.mobilePhone());
        customer.setEmail(command.email());
        customer.setAddressLine1(command.addressLine1());
        customer.setAddressLine2(command.addressLine2());
        customer.setCity(command.city());
        customer.setPostcode(command.postcode());
        customerRepository.save(customer);

        auditService.logEvent(customerId, "CONTACT_DETAILS_UPDATED", ipAddress,
            Map.of("email", command.email()));
    }
}
