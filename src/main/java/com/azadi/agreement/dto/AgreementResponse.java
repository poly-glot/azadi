package com.azadi.agreement.dto;

import com.azadi.agreement.Agreement;

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
            formatPence(agreement.getBalancePence()),
            agreement.getApr().toPlainString() + "%",
            agreement.getOriginalTermMonths(),
            agreement.getContractMileage(),
            formatPence(agreement.getExcessPricePerMilePence()),
            agreement.getVehicleModel(),
            agreement.getRegistration(),
            formatPence(agreement.getLastPaymentPence()),
            agreement.getLastPaymentDate(),
            formatPence(agreement.getNextPaymentPence()),
            agreement.getNextPaymentDate(),
            agreement.getPaymentsRemaining(),
            agreement.getFinalPaymentDate()
        );
    }

    private static String formatPence(long pence) {
        return String.format("\u00A3%,.2f", pence / 100.0);
    }
}
