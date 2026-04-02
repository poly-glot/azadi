package com.azadi.settlement;

import com.azadi.agreement.AgreementService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class SettlementService {

    private static final long EARLY_SETTLEMENT_FEE_BPS = 200;
    private static final int SETTLEMENT_VALIDITY_DAYS = 28;

    private final SettlementRepository settlementRepository;
    private final AgreementService agreementService;

    public SettlementService(SettlementRepository settlementRepository,
                             AgreementService agreementService) {
        this.settlementRepository = settlementRepository;
        this.agreementService = agreementService;
    }

    public SettlementFigure calculateSettlement(String customerId, Long agreementId) {
        var agreement = agreementService.getAgreement(customerId, agreementId);

        var earlySettlementFee = agreement.getBalancePence() * EARLY_SETTLEMENT_FEE_BPS / 10_000;
        var totalSettlement = agreement.getBalancePence() + earlySettlementFee;

        var settlement = new SettlementFigure();
        settlement.setAgreementId(agreementId);
        settlement.setCustomerId(customerId);
        settlement.setAmountPence(totalSettlement);
        settlement.setCalculatedAt(Instant.now());
        settlement.setValidUntil(LocalDate.now().plusDays(SETTLEMENT_VALIDITY_DAYS));

        return settlementRepository.save(settlement);
    }

    public List<SettlementFigure> getSettlementsForCustomer(String customerId) {
        return settlementRepository.findByCustomerId(customerId);
    }
}
