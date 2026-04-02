package com.azadi.document;

import com.google.cloud.spring.data.datastore.repository.DatastoreRepository;

import java.util.List;

public interface DocumentRepository extends DatastoreRepository<Document, Long> {

    List<Document> findByCustomerId(String customerId);
}
