package com.azadi.settlement.dto;

import com.azadi.common.MoneyFormatter;
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
            MoneyFormatter.formatPence(figure.getAmountPence()),
            figure.getCalculatedAt(),
            figure.getValidUntil()
        );
    }
}
