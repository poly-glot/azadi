package com.azadi.audit;

import com.google.cloud.spring.data.datastore.repository.DatastoreRepository;

import java.util.List;

public interface AuditRepository extends DatastoreRepository<AuditEvent, Long> {

    List<AuditEvent> findByCustomerId(String customerId);
}
