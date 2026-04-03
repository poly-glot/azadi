package com.azadi.agreement.dto;

import com.azadi.agreement.Agreement;
import com.azadi.common.MoneyFormatter;

import java.time.LocalDate;

public record AgreementResponse(
    Long id,
    String agreementNumber,
    String type,
    String balance,
    String apr,
    int originalTermMonths,
    int contractMileage,
    String excessPricePerMile,
    String vehicleModel,
    String registration,
    String lastPayment,
    LocalDate lastPaymentDate,
    String nextPayment,
    LocalDate nextPaymentDate,
    int paymentsRemaining,
    LocalDate finalPaymentDate
) {

    public static AgreementResponse from(Agreement agreement) {
        return new AgreementResponse(
            agreement.getId(),
            agreement.getAgreementNumber(),
            agreement.getType(),
            MoneyFormatter.formatPence(agreement.getBalancePence()),
            agreement.getApr() != null ? agreement.getApr() + "%" : "N/A",
            agreement.getOriginalTermMonths(),
            agreement.getContractMileage(),
            MoneyFormatter.formatPence(agreement.getExcessPricePerMilePence()),
            agreement.getVehicleModel(),
            agreement.getRegistration(),
            MoneyFormatter.formatPence(agreement.getLastPaymentPence()),
            agreement.getLastPaymentDate(),
            MoneyFormatter.formatPence(agreement.getNextPaymentPence()),
            agreement.getNextPaymentDate(),
            agreement.getPaymentsRemaining(),
            agreement.getFinalPaymentDate()
        );
    }
}
