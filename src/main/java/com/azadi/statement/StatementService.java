package com.azadi.statement;

import com.azadi.audit.AuditService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class StatementService {

    private final StatementRepository statementRepository;
    private final AuditService auditService;

    public StatementService(StatementRepository statementRepository,
                            AuditService auditService) {
        this.statementRepository = statementRepository;
        this.auditService = auditService;
    }

    public StatementRequest requestStatement(String customerId, Long agreementId, String ipAddress) {
        var request = new StatementRequest();
        request.setCustomerId(customerId);
        request.setAgreementId(agreementId);
        request.setStatus(StatementRequest.STATUS_PENDING);
        request.setRequestedAt(Instant.now());

        var saved = statementRepository.save(request);

        auditService.logEvent(customerId, "STATEMENT_REQUESTED", ipAddress,
            Map.of("agreementId", String.valueOf(agreementId)));

        return saved;
    }

    public List<StatementRequest> getStatementsForCustomer(String customerId) {
        return statementRepository.findByCustomerId(customerId);
    }
}
