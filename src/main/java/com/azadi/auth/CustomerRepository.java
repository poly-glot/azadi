package com.azadi.auth;

import com.google.cloud.spring.data.datastore.repository.DatastoreRepository;

import java.util.Optional;

public interface CustomerRepository extends DatastoreRepository<Customer, Long> {

    Optional<Customer> findByCustomerId(String customerId);
}
