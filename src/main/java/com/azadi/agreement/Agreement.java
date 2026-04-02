package com.azadi.agreement;

import com.google.cloud.spring.data.datastore.core.mapping.Entity;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity(name = "Agreement")
public class Agreement {

    @Id
    private Long id;
    private String agreementNumber;
    private String customerId;
    private String type;
    private long balancePence;
    private BigDecimal apr;
    private int originalTermMonths;
    private int contractMileage;
    private long excessPricePerMilePence;
    private String vehicleModel;
    private String registration;
    private long lastPaymentPence;
    private LocalDate lastPaymentDate;
    private long nextPaymentPence;
    private LocalDate nextPaymentDate;
    private int paymentsRemaining;
    private LocalDate finalPaymentDate;
    private boolean paymentDateChanged;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAgreementNumber() {
        return agreementNumber;
    }

    public void setAgreementNumber(String agreementNumber) {
        this.agreementNumber = agreementNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getBalancePence() {
        return balancePence;
    }

    public void setBalancePence(long balancePence) {
        this.balancePence = balancePence;
    }

    public BigDecimal getApr() {
        return apr;
    }

    public void setApr(BigDecimal apr) {
        this.apr = apr;
    }

    public int getOriginalTermMonths() {
        return originalTermMonths;
    }

    public void setOriginalTermMonths(int originalTermMonths) {
        this.originalTermMonths = originalTermMonths;
    }

    public int getContractMileage() {
        return contractMileage;
    }

    public void setContractMileage(int contractMileage) {
        this.contractMileage = contractMileage;
    }

    public long getExcessPricePerMilePence() {
        return excessPricePerMilePence;
    }

    public void setExcessPricePerMilePence(long excessPricePerMilePence) {
        this.excessPricePerMilePence = excessPricePerMilePence;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getRegistration() {
        return registration;
    }

    public void setRegistration(String registration) {
        this.registration = registration;
    }

    public long getLastPaymentPence() {
        return lastPaymentPence;
    }

    public void setLastPaymentPence(long lastPaymentPence) {
        this.lastPaymentPence = lastPaymentPence;
    }

    public LocalDate getLastPaymentDate() {
        return lastPaymentDate;
    }

    public void setLastPaymentDate(LocalDate lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
    }

    public long getNextPaymentPence() {
        return nextPaymentPence;
    }

    public void setNextPaymentPence(long nextPaymentPence) {
        this.nextPaymentPence = nextPaymentPence;
    }

    public LocalDate getNextPaymentDate() {
        return nextPaymentDate;
    }

    public void setNextPaymentDate(LocalDate nextPaymentDate) {
        this.nextPaymentDate = nextPaymentDate;
    }

    public int getPaymentsRemaining() {
        return paymentsRemaining;
    }

    public void setPaymentsRemaining(int paymentsRemaining) {
        this.paymentsRemaining = paymentsRemaining;
    }

    public LocalDate getFinalPaymentDate() {
        return finalPaymentDate;
    }

    public void setFinalPaymentDate(LocalDate finalPaymentDate) {
        this.finalPaymentDate = finalPaymentDate;
    }

    public boolean isPaymentDateChanged() {
        return paymentDateChanged;
    }

    public void setPaymentDateChanged(boolean paymentDateChanged) {
        this.paymentDateChanged = paymentDateChanged;
    }
}
