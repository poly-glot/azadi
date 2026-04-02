package com.azadi.agreement;

import com.google.cloud.spring.data.datastore.repository.DatastoreRepository;

import java.util.List;
import java.util.Optional;

public interface AgreementRepository extends DatastoreRepository<Agreement, Long> {

    List<Agreement> findByCustomerId(String customerId);

    Optional<Agreement> findByAgreementNumber(String agreementNumber);
}
