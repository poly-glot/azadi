package com.azadi.settlement;

import com.google.cloud.spring.data.datastore.core.mapping.Entity;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.time.LocalDate;

@Entity(name = "SettlementFigure")
public class SettlementFigure {

    @Id
    private Long id;
    private Long agreementId;
    private String customerId;
    private long amountPence;
    private Instant calculatedAt;
    private LocalDate validUntil;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAgreementId() {
        return agreementId;
    }

    public void setAgreementId(Long agreementId) {
        this.agreementId = agreementId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public long getAmountPence() {
        return amountPence;
    }

    public void setAmountPence(long amountPence) {
        this.amountPence = amountPence;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }
}
