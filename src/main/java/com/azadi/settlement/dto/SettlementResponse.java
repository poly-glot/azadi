package com.azadi.settlement.dto;

import com.azadi.settlement.SettlementFigure;

import java.time.Instant;
import java.time.LocalDate;

public record SettlementResponse(
    Long id,
    Long agreementId,
    String amount,
    Instant calculatedAt,
    LocalDate validUntil
) {

    public static SettlementResponse from(SettlementFigure figure) {
        return new SettlementResponse(
            figure.getId(),
            figure.getAgreementId(),
            String.format("\u00A3%,.2f", figure.getAmountPence() / 100.0),
            figure.getCalculatedAt(),
            figure.getValidUntil()
        );
    }
}
