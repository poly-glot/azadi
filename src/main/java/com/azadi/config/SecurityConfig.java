package com.azadi.config;

import com.azadi.auth.AzadiAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.HeaderWriterFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final AzadiAuthenticationProvider azadiAuthenticationProvider;
    private final RateLimitFilter rateLimitFilter;
    private final CspNonceFilter cspNonceFilter;

    public SecurityConfig(AzadiAuthenticationProvider azadiAuthenticationProvider,
                          RateLimitFilter rateLimitFilter,
                          CspNonceFilter cspNonceFilter) {
        this.azadiAuthenticationProvider = azadiAuthenticationProvider;
        this.rateLimitFilter = rateLimitFilter;
        this.cspNonceFilter = cspNonceFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(azadiAuthenticationProvider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/login-error", "/assets/**",
                    "/cookies", "/privacy", "/terms",
                    "/actuator/health", "/api/stripe/webhook", "/api/csp-report").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/my-account", true)
                .failureUrl("/login-error")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .deleteCookies("AZADI_SESSION")
            )
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
                .maximumSessions(1)
            )
            .headers(headers -> headers
                .contentTypeOptions(nosniff -> { })
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(63_072_000)
                    .includeSubDomains(true)
                )
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                .permissionsPolicy(permissions -> permissions
                    .policy("camera=(), microphone=(), geolocation=()")
                )
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/stripe/webhook")
            )
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(cspNonceFilter, HeaderWriterFilter.class);

        return http.build();
    }
}
