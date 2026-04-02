package com.azadi.statement;

import com.google.cloud.spring.data.datastore.repository.DatastoreRepository;

import java.util.List;

public interface StatementRepository extends DatastoreRepository<StatementRequest, Long> {

    List<StatementRequest> findByCustomerId(String customerId);
}
