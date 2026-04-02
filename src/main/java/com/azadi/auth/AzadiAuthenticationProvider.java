package com.azadi.auth;

import com.azadi.agreement.AgreementRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Component
public class AzadiAuthenticationProvider implements AuthenticationProvider {

    private static final DateTimeFormatter DOB_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AgreementRepository agreementRepository;
    private final CustomerRepository customerRepository;
    private final LoginAttemptTracker loginAttemptTracker;

    public AzadiAuthenticationProvider(AgreementRepository agreementRepository,
                                       CustomerRepository customerRepository,
                                       LoginAttemptTracker loginAttemptTracker) {
        this.agreementRepository = agreementRepository;
        this.customerRepository = customerRepository;
        this.loginAttemptTracker = loginAttemptTracker;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        var agreementNumber = authentication.getName();
        var credentials = String.valueOf(authentication.getCredentials());

        if (loginAttemptTracker.isBlocked(agreementNumber)) {
            throw new BadCredentialsException("Account temporarily locked due to too many failed attempts.");
        }

        var parts = credentials.split("\\|", 2);
        if (parts.length != 2) {
            loginAttemptTracker.recordFailure(agreementNumber);
            throw new BadCredentialsException("Invalid credentials format.");
        }

        var dobString = parts[0];
        var postcode = parts[1];

        LocalDate dob;
        try {
            dob = LocalDate.parse(dobString, DOB_FORMAT);
        } catch (DateTimeParseException e) {
            loginAttemptTracker.recordFailure(agreementNumber);
            throw new BadCredentialsException("Invalid date of birth format.", e);
        }

        var matchingAgreement = agreementRepository.findByAgreementNumber(agreementNumber)
            .orElse(null);

        if (matchingAgreement == null) {
            loginAttemptTracker.recordFailure(agreementNumber);
            throw new BadCredentialsException("Invalid agreement number, date of birth, or postcode.");
        }

        var customerId = matchingAgreement.getCustomerId();

        var customer = customerRepository.findByCustomerId(customerId)
            .orElse(null);

        if (customer == null) {
            loginAttemptTracker.recordFailure(agreementNumber);
            throw new BadCredentialsException("Invalid agreement number, date of birth, or postcode.");
        }

        var normalizedInputPostcode = postcode.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        var normalizedStoredPostcode = customer.getPostcode().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);

        if (!dob.equals(customer.getDob()) || !normalizedInputPostcode.equals(normalizedStoredPostcode)) {
            loginAttemptTracker.recordFailure(agreementNumber);
            throw new BadCredentialsException("Invalid agreement number, date of birth, or postcode.");
        }

        loginAttemptTracker.recordSuccess(agreementNumber);

        var userDetails = new CustomUserDetails(customerId, customer.getFullName(), agreementNumber);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));

        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
