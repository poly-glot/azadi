package com.azadi.bank;

import com.google.cloud.spring.data.datastore.repository.DatastoreRepository;

import java.util.Optional;

public interface BankDetailsRepository extends DatastoreRepository<BankDetails, Long> {

    Optional<BankDetails> findByCustomerId(String customerId);
}
