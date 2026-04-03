package com.azadi.seed;

import com.azadi.agreement.Agreement;
import com.azadi.auth.Customer;
import com.azadi.bank.BankDetails;
import com.azadi.bank.BankDetailsEncryptor;
import com.azadi.document.Document;
import com.azadi.payment.PaymentRecord;
import com.azadi.settlement.SettlementFigure;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.google.cloud.spring.data.datastore.core.DatastoreTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the Firestore emulator with test data on startup.
 *
 * <p>Data is read from {@code src/main/resources/seed/customers.json} --
 * a plain JSON file that frontend developers can edit without touching Java.
 * Add a customer, change a name, adjust a balance: edit the JSON, restart
 * Spring Boot, and the emulator is re-populated.</p>
 *
 * <p>Only runs when the {@code dev} profile is active. Skips seeding if
 * a {@link SeedMarker} entity already exists (delete the emulator data
 * or restart Docker to re-seed).</p>
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DataSeeder.class);
    private static final String SEED_FILE = "seed/customers.json";

    private final DatastoreTemplate datastoreTemplate;
    private final BankDetailsEncryptor encryptor;
    private final JsonMapper objectMapper;
    private final boolean seedEnabled;

    public DataSeeder(DatastoreTemplate datastoreTemplate,
                      BankDetailsEncryptor encryptor,
                      JsonMapper objectMapper,
                      @Value("${azadi.seed-data:false}") boolean seedEnabled) {
        this.datastoreTemplate = datastoreTemplate;
        this.encryptor = encryptor;
        this.objectMapper = objectMapper;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!seedEnabled) {
            return;
        }
        if (datastoreTemplate.findAll(SeedMarker.class).iterator().hasNext()) {
            LOG.info("Seed data already exists, skipping. "
                + "To re-seed: docker compose down && docker compose up -d, then restart Spring Boot.");
            return;
        }

        LOG.info("Reading seed data from {}...", SEED_FILE);
        var resource = new ClassPathResource(SEED_FILE);
        List<JsonNode> customers = objectMapper.readValue(
            resource.getInputStream(), new TypeReference<>() {});

        for (var node : customers) {
            seedCustomer(node);
        }

        var marker = new SeedMarker();
        marker.setSeededAt(Instant.now());
        datastoreTemplate.save(marker);

        LOG.info("Seeded {} customers from {}.", customers.size(), SEED_FILE);
    }

    private void seedCustomer(JsonNode node) {
        var customerId = node.get("customerId").asText();

        var customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setFullName(node.get("fullName").asText());
        customer.setEmail(node.get("email").asText());
        customer.setDob(LocalDate.parse(node.get("dob").asText()));
        customer.setPostcode(node.get("postcode").asText());
        customer.setPhone(node.get("phone").asText());
        customer.setAddressLine1(node.get("addressLine1").asText());
        customer.setAddressLine2(node.path("addressLine2").asText(null));
        customer.setCity(node.get("city").asText());
        datastoreTemplate.save(customer);

        var agr = node.get("agreement");
        var agreement = new Agreement();
        agreement.setAgreementNumber(agr.get("agreementNumber").asText());
        agreement.setCustomerId(customerId);
        agreement.setType(agr.get("type").asText());
        agreement.setBalancePence(agr.get("balancePence").asLong());
        agreement.setApr(agr.get("apr").asText());
        agreement.setOriginalTermMonths(agr.get("termMonths").asInt());
        agreement.setContractMileage(agr.get("contractMileage").asInt());
        agreement.setExcessPricePerMilePence(agr.get("excessPricePerMilePence").asLong());
        agreement.setVehicleModel(agr.get("vehicleModel").asText());
        agreement.setRegistration(agr.get("registration").asText());
        agreement.setLastPaymentPence(agr.get("monthlyPaymentPence").asLong());
        agreement.setLastPaymentDate(LocalDate.now().minusMonths(1));
        agreement.setNextPaymentPence(agr.get("monthlyPaymentPence").asLong());
        agreement.setNextPaymentDate(LocalDate.now().plusDays(14));
        agreement.setPaymentsRemaining(agr.get("paymentsRemaining").asInt());
        agreement.setFinalPaymentDate(LocalDate.now().plusMonths(agr.get("paymentsRemaining").asInt()));
        var savedAgreement = datastoreTemplate.save(agreement);

        seedPaymentHistory(customerId, savedAgreement.getId());
        seedDocuments(customerId, node.get("documents"));
        seedSettlement(customerId, savedAgreement.getId(), agr.get("balancePence").asLong());

        var bank = node.get("bankDetails");
        seedBankDetails(customerId, node.get("fullName").asText(),
            bank.get("accountNumber").asText(), bank.get("sortCode").asText());
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

    private void seedDocuments(String customerId, JsonNode docsNode) {
        if (docsNode == null || !docsNode.isArray()) {
            return;
        }
        for (var docNode : docsNode) {
            var doc = new Document();
            doc.setCustomerId(customerId);
            doc.setTitle(docNode.get("title").asText());
            doc.setFileName(docNode.get("fileName").asText());
            doc.setContentType(docNode.get("contentType").asText());
            doc.setStoragePath("documents/" + customerId + "/" + docNode.get("fileName").asText());
            doc.setFileSizeBytes(docNode.get("fileSizeBytes").asLong());
            doc.setCreatedAt(Instant.now().minusSeconds(180L * 24 * 3600));
            datastoreTemplate.save(doc);
        }
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
