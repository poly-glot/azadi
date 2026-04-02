package com.azadi.seed;

import com.azadi.agreement.Agreement;
import com.azadi.auth.Customer;
import com.azadi.bank.BankDetails;
import com.azadi.bank.BankDetailsEncryptor;
import com.azadi.document.Document;
import com.azadi.payment.PaymentRecord;
import com.azadi.settlement.SettlementFigure;
import com.google.cloud.spring.data.datastore.core.DatastoreTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DataSeeder.class);

    private final DatastoreTemplate datastoreTemplate;
    private final BankDetailsEncryptor encryptor;

    public DataSeeder(DatastoreTemplate datastoreTemplate, BankDetailsEncryptor encryptor) {
        this.datastoreTemplate = datastoreTemplate;
        this.encryptor = encryptor;
    }

    @Override
    public void run(String... args) {
        var existing = datastoreTemplate.findAll(SeedMarker.class);
        if (existing.iterator().hasNext()) {
            LOG.info("Seed data already exists, skipping.");
            return;
        }

        LOG.info("Seeding development data...");

        seedCustomer1();
        seedCustomer2();
        seedCustomer3();
        seedCustomer4();
        seedCustomer5();

        var marker = new SeedMarker();
        marker.setSeededAt(Instant.now());
        datastoreTemplate.save(marker);

        LOG.info("Seed data created successfully.");
    }

    private void seedCustomer1() {
        var customer = createCustomer("CUST-001", "James Wilson", "james.wilson@email.com",
            LocalDate.of(1985, 3, 15), "SW1A 1AA", "07700 900001",
            "10 Downing Street", null, "London");

        var agreement = createAgreement("AGR-100001", "CUST-001", "PCP",
            6_499_960L, new BigDecimal("10.0"), 49, 40_000, 15L,
            "AZADI SUMMIT V8 TOURING", "AB68 ABC",
            132_652L, LocalDate.now().minusMonths(1),
            132_652L, LocalDate.now().plusDays(14), 37,
            LocalDate.now().plusMonths(37));

        seedPaymentHistory("CUST-001", agreement.getId());
        seedDocuments("CUST-001");
        seedSettlement("CUST-001", agreement.getId(), 6_499_960L);
        seedBankDetails("CUST-001", "James Wilson", "12345678", "20-30-40");
    }

    private void seedCustomer2() {
        var customer = createCustomer("CUST-002", "Sarah Thompson", "sarah.thompson@email.com",
            LocalDate.of(1990, 7, 22), "M1 1AE", "07700 900002",
            "1 Piccadilly", "City Centre", "Manchester");

        var agreement = createAgreement("AGR-100002", "CUST-002", "HP",
            1_875_000L, new BigDecimal("8.5"), 36, 30_000, 12L,
            "AZADI EXPLORER SPORT", "CD19 XYZ",
            52_083L, LocalDate.now().minusMonths(1),
            52_083L, LocalDate.now().plusDays(14), 24,
            LocalDate.now().plusMonths(24));

        seedPaymentHistory("CUST-002", agreement.getId());
        seedDocuments("CUST-002");
        seedSettlement("CUST-002", agreement.getId(), 1_875_000L);
        seedBankDetails("CUST-002", "Sarah Thompson", "87654321", "10-20-30");
    }

    private void seedCustomer3() {
        var customer = createCustomer("CUST-003", "David Patel", "david.patel@email.com",
            LocalDate.of(1978, 11, 3), "B1 1BB", "07700 900003",
            "1 Victoria Square", null, "Birmingham");

        var agreement = createAgreement("AGR-100003", "CUST-003", "LEASE",
            2_210_000L, new BigDecimal("6.9"), 48, 35_000, 10L,
            "AZADI VANGUARD HSE", "EF20 DEF",
            46_042L, LocalDate.now().minusMonths(1),
            46_042L, LocalDate.now().plusDays(14), 36,
            LocalDate.now().plusMonths(36));

        seedPaymentHistory("CUST-003", agreement.getId());
        seedDocuments("CUST-003");
        seedSettlement("CUST-003", agreement.getId(), 2_210_000L);
        seedBankDetails("CUST-003", "David Patel", "11223344", "40-50-60");
    }

    private void seedCustomer4() {
        var customer = createCustomer("CUST-004", "Emma Roberts", "emma.roberts@email.com",
            LocalDate.of(1992, 1, 28), "LS1 1BA", "07700 900004",
            "1 City Square", null, "Leeds");

        var agreement = createAgreement("AGR-100004", "CUST-004", "PCP",
            1_420_000L, new BigDecimal("9.2"), 42, 25_000, 14L,
            "AZADI PIONEER SE", "GH21 GHI",
            33_810L, LocalDate.now().minusMonths(1),
            33_810L, LocalDate.now().plusDays(14), 30,
            LocalDate.now().plusMonths(30));

        seedPaymentHistory("CUST-004", agreement.getId());
        seedDocuments("CUST-004");
        seedSettlement("CUST-004", agreement.getId(), 1_420_000L);
        seedBankDetails("CUST-004", "Emma Roberts", "55667788", "60-70-80");
    }

    private void seedCustomer5() {
        var customer = createCustomer("CUST-005", "Michael Chen", "michael.chen@email.com",
            LocalDate.of(1988, 6, 10), "EH1 1YZ", "07700 900005",
            "1 Princes Street", null, "Edinburgh");

        var agreement = createAgreement("AGR-100005", "CUST-005", "HP",
            1_680_000L, new BigDecimal("7.5"), 36, 20_000, 11L,
            "AZADI MERIDIAN R-DYNAMIC", "IJ22 JKL",
            46_667L, LocalDate.now().minusMonths(1),
            46_667L, LocalDate.now().plusDays(14), 24,
            LocalDate.now().plusMonths(24));

        seedPaymentHistory("CUST-005", agreement.getId());
        seedDocuments("CUST-005");
        seedSettlement("CUST-005", agreement.getId(), 1_680_000L);
        seedBankDetails("CUST-005", "Michael Chen", "99001122", "80-90-00");
    }

    private Customer createCustomer(String customerId, String fullName, String email,
                                     LocalDate dob, String postcode, String phone,
                                     String addressLine1, String addressLine2, String city) {
        var customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setFullName(fullName);
        customer.setEmail(email);
        customer.setDob(dob);
        customer.setPostcode(postcode);
        customer.setPhone(phone);
        customer.setAddressLine1(addressLine1);
        customer.setAddressLine2(addressLine2);
        customer.setCity(city);
        return datastoreTemplate.save(customer);
    }

    private Agreement createAgreement(String agreementNumber, String customerId, String type,
                                       long balancePence, BigDecimal apr, int termMonths,
                                       int contractMileage, long excessPricePence,
                                       String vehicleModel, String registration,
                                       long lastPaymentPence, LocalDate lastPaymentDate,
                                       long nextPaymentPence, LocalDate nextPaymentDate,
                                       int paymentsRemaining, LocalDate finalPaymentDate) {
        var agreement = new Agreement();
        agreement.setAgreementNumber(agreementNumber);
        agreement.setCustomerId(customerId);
        agreement.setType(type);
        agreement.setBalancePence(balancePence);
        agreement.setApr(apr);
        agreement.setOriginalTermMonths(termMonths);
        agreement.setContractMileage(contractMileage);
        agreement.setExcessPricePerMilePence(excessPricePence);
        agreement.setVehicleModel(vehicleModel);
        agreement.setRegistration(registration);
        agreement.setLastPaymentPence(lastPaymentPence);
        agreement.setLastPaymentDate(lastPaymentDate);
        agreement.setNextPaymentPence(nextPaymentPence);
        agreement.setNextPaymentDate(nextPaymentDate);
        agreement.setPaymentsRemaining(paymentsRemaining);
        agreement.setFinalPaymentDate(finalPaymentDate);
        return datastoreTemplate.save(agreement);
    }

    private void seedPaymentHistory(String customerId, Long agreementId) {
        var payments = new ArrayList<PaymentRecord>();
        for (int i = 6; i >= 1; i--) {
            var payment = new PaymentRecord();
            payment.setAgreementId(agreementId);
            payment.setCustomerId(customerId);
            payment.setAmountPence(50_000L + (i * 1_000L));
            payment.setStripePaymentIntentId("pi_seed_" + customerId + "_" + i);
            payment.setStatus(PaymentRecord.STATUS_COMPLETED);
            payment.setCreatedAt(Instant.now().minusSeconds((long) i * 30 * 24 * 3600));
            payment.setCompletedAt(Instant.now().minusSeconds((long) i * 30 * 24 * 3600 - 60));
            payments.add(payment);
        }
        datastoreTemplate.saveAll(payments);
    }

    private void seedDocuments(String customerId) {
        var doc1 = new Document();
        doc1.setCustomerId(customerId);
        doc1.setTitle("Finance Agreement");
        doc1.setFileName("finance-agreement.pdf");
        doc1.setContentType("application/pdf");
        doc1.setStoragePath("documents/" + customerId + "/finance-agreement.pdf");
        doc1.setFileSizeBytes(245_000L);
        doc1.setCreatedAt(Instant.now().minusSeconds(180L * 24 * 3600));

        var doc2 = new Document();
        doc2.setCustomerId(customerId);
        doc2.setTitle("Welcome Pack");
        doc2.setFileName("welcome-pack.pdf");
        doc2.setContentType("application/pdf");
        doc2.setStoragePath("documents/" + customerId + "/welcome-pack.pdf");
        doc2.setFileSizeBytes(128_000L);
        doc2.setCreatedAt(Instant.now().minusSeconds(180L * 24 * 3600));

        datastoreTemplate.save(doc1);
        datastoreTemplate.save(doc2);
    }

    private void seedSettlement(String customerId, Long agreementId, long balancePence) {
        var settlement = new SettlementFigure();
        settlement.setAgreementId(agreementId);
        settlement.setCustomerId(customerId);
        settlement.setAmountPence(balancePence + (long) (balancePence * 0.02));
        settlement.setCalculatedAt(Instant.now());
        settlement.setValidUntil(LocalDate.now().plusDays(28));
        datastoreTemplate.save(settlement);
    }

    private void seedBankDetails(String customerId, String holderName,
                                  String accountNumber, String sortCode) {
        var bankDetails = new BankDetails();
        bankDetails.setCustomerId(customerId);
        bankDetails.setAccountHolderName(holderName);
        bankDetails.setEncryptedAccountNumber(encryptor.encrypt(accountNumber));
        bankDetails.setEncryptedSortCode(encryptor.encrypt(sortCode));
        bankDetails.setLastFourAccount(accountNumber.substring(4));
        bankDetails.setLastTwoSortCode(sortCode.substring(sortCode.length() - 2));
        bankDetails.setUpdatedAt(Instant.now());
        datastoreTemplate.save(bankDetails);
    }
}
