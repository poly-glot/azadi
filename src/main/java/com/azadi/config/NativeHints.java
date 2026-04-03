package com.azadi.config;

import com.azadi.agreement.Agreement;
import com.azadi.agreement.dto.AgreementResponse;
import com.azadi.auth.Customer;
import com.azadi.bank.BankDetails;
import com.azadi.document.Document;
import com.azadi.settlement.SettlementFigure;
import com.azadi.settlement.dto.SettlementResponse;
import com.azadi.statement.StatementRequest;
import com.azadi.payment.PaymentRecord;
import com.azadi.common.OrdinalFormat;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers reflection hints for classes accessed via SpEL in Thymeleaf templates.
 * AOT cannot detect these usages automatically.
 */
@Configuration
@ImportRuntimeHints(NativeHints.TemplateHints.class)
public class NativeHints {

    static class TemplateHints implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // DTOs and entities used in Thymeleaf SpEL expressions
            for (var type : new Class<?>[] {
                AgreementResponse.class,
                SettlementResponse.class,
                Agreement.class,
                Customer.class,
                BankDetails.class,
                Document.class,
                SettlementFigure.class,
                StatementRequest.class,
                PaymentRecord.class,
                OrdinalFormat.class
            }) {
                hints.reflection().registerType(type,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    MemberCategory.DECLARED_FIELDS);
            }

            // JDK collection types used via SpEL (.size(), .empty, iteration)
            var jdkCollections = new ArrayList<Class<?>>();
            jdkCollections.add(java.util.ArrayList.class);
            jdkCollections.add(java.util.Collections.emptyList().getClass());
            jdkCollections.add(List.of().getClass());
            jdkCollections.add(List.of(1).getClass());
            jdkCollections.add(List.of(1, 2).getClass());
            try {
                jdkCollections.add(Class.forName("java.util.ImmutableCollections$ListN"));
                jdkCollections.add(Class.forName("java.util.ImmutableCollections$List12"));
                jdkCollections.add(Class.forName("java.util.ImmutableCollections$SubList"));
            } catch (ClassNotFoundException ignored) {
            }

            for (var type : jdkCollections) {
                hints.reflection().registerType(type,
                    MemberCategory.INVOKE_PUBLIC_METHODS);
            }
        }
    }
}
