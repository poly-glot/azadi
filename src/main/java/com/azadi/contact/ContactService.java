package com.azadi.contact;

import com.azadi.audit.AuditService;
import com.azadi.auth.Customer;
import com.azadi.auth.CustomerRepository;
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

    public void updateContactDetails(String customerId, String phone, String email,
                                     String addressLine1, String addressLine2,
                                     String city, String postcode, String ipAddress) {
        var customer = getCustomer(customerId);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setAddressLine1(addressLine1);
        customer.setAddressLine2(addressLine2);
        customer.setCity(city);
        customer.setPostcode(postcode);
        customerRepository.save(customer);

        auditService.logEvent(customerId, "CONTACT_DETAILS_UPDATED", ipAddress,
            Map.of("email", email));
    }
}
