package com.azadi.settlement;

import com.azadi.agreement.AgreementService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final AgreementService agreementService;

    public SettlementService(SettlementRepository settlementRepository,
                             AgreementService agreementService) {
        this.settlementRepository = settlementRepository;
        this.agreementService = agreementService;
    }

    public SettlementFigure calculateSettlement(String customerId, Long agreementId) {
        var agreement = agreementService.getAgreement(customerId, agreementId);

        // Simple settlement calculation: balance + early settlement fee (2% of balance)
        var earlySettlementFee = (long) (agreement.getBalancePence() * 0.02);
        var totalSettlement = agreement.getBalancePence() + earlySettlementFee;

        var settlement = new SettlementFigure();
        settlement.setAgreementId(agreementId);
        settlement.setCustomerId(customerId);
        settlement.setAmountPence(totalSettlement);
        settlement.setCalculatedAt(Instant.now());
        settlement.setValidUntil(LocalDate.now().plusDays(28));

        return settlementRepository.save(settlement);
    }

    public List<SettlementFigure> getSettlementsForCustomer(String customerId) {
        return settlementRepository.findByCustomerId(customerId);
    }
}
