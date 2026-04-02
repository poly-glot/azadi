package com.azadi.bank;

import com.google.cloud.spring.data.datastore.core.mapping.Entity;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Entity(name = "BankDetails")
public class BankDetails {

    @Id
    private Long id;
    private String customerId;
    private String accountHolderName;
    private String encryptedAccountNumber;
    private String encryptedSortCode;
    private String lastFourAccount;
    private String lastTwoSortCode;
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getEncryptedAccountNumber() {
        return encryptedAccountNumber;
    }

    public void setEncryptedAccountNumber(String encryptedAccountNumber) {
        this.encryptedAccountNumber = encryptedAccountNumber;
    }

    public String getEncryptedSortCode() {
        return encryptedSortCode;
    }

    public void setEncryptedSortCode(String encryptedSortCode) {
        this.encryptedSortCode = encryptedSortCode;
    }

    public String getLastFourAccount() {
        return lastFourAccount;
    }

    public void setLastFourAccount(String lastFourAccount) {
        this.lastFourAccount = lastFourAccount;
    }

    public String getLastTwoSortCode() {
        return lastTwoSortCode;
    }

    public void setLastTwoSortCode(String lastTwoSortCode) {
        this.lastTwoSortCode = lastTwoSortCode;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
