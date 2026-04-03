package com.azadi.payment;

import com.azadi.agreement.Agreement;
import com.azadi.agreement.AgreementService;
import com.azadi.audit.AuditService;
import com.azadi.auth.Customer;
import com.azadi.auth.CustomerRepository;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private StripePaymentService stripePaymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AgreementService agreementService;

    private PaymentService paymentService;

    private static final String CUSTOMER_ID = "CUST-001";
    private static final String AGREEMENT_NUMBER = "AGR-100001";
    private static final long AMOUNT_PENCE = 112732L;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
            stripePaymentService, paymentRepository,
            auditService, customerRepository, agreementService);
    }

    @Test
    @DisplayName("initiatePayment creates Stripe intent and saves payment record")
    void initiatePaymentCreatesIntentAndSaves() throws Exception {
        // Arrange
        var agreement = new Agreement();
        agreement.setId(1L);
        agreement.setAgreementNumber(AGREEMENT_NUMBER);
        agreement.setCustomerId(CUSTOMER_ID);
        when(agreementService.getAgreement(CUSTOMER_ID, 1L)).thenReturn(agreement);

        var customer = new Customer();
        customer.setEmail("test@test.com");
        when(customerRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        var mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getId()).thenReturn("pi_test_123");
        when(stripePaymentService.createPaymentIntent(AMOUNT_PENCE, AGREEMENT_NUMBER, "test@test.com"))
            .thenReturn(mockIntent);
        when(paymentRepository.save(any(PaymentRecord.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // Act
        var result = paymentService.initiatePayment(
            CUSTOMER_ID, 1L, AMOUNT_PENCE, "127.0.0.1");

        // Assert
        assertThat(result.getId()).isEqualTo("pi_test_123");
        verify(paymentRepository).save(any(PaymentRecord.class));
        verify(auditService).logEvent(eq(CUSTOMER_ID), eq("PAYMENT_INITIATED"), eq("127.0.0.1"), any());
    }
}
