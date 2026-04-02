package com.azadi.settlement;

import com.google.cloud.spring.data.datastore.repository.DatastoreRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends DatastoreRepository<SettlementFigure, Long> {

    List<SettlementFigure> findByCustomerId(String customerId);

    Optional<SettlementFigure> findByAgreementId(Long agreementId);
}
