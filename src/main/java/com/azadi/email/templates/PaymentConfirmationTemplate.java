package com.azadi.email.templates;

public final class PaymentConfirmationTemplate {

    private PaymentConfirmationTemplate() {
    }

    public static String build(long amountPence) {
        var amount = String.format("\u00A3%,.2f", amountPence / 100.0);
        return EmailLayoutTemplate.wrap("Payment Confirmation", """
            <h1 style="color:#0c121d;font-family:'Poppins',Arial,sans-serif;font-size:24px;margin:0 0 16px;">
                Payment Confirmed
            </h1>
            <p style="color:#0c121d;font-family:'Poppins',Arial,sans-serif;font-size:16px;line-height:1.5;">
                Your payment of <strong>%s</strong> has been successfully processed.
            </p>
            <p style="color:#0c121d;font-family:'Poppins',Arial,sans-serif;font-size:16px;line-height:1.5;">
                This will be reflected in your account within 2-3 business days.
            </p>
            <p style="color:#666;font-family:'Poppins',Arial,sans-serif;font-size:14px;line-height:1.5;margin-top:24px;">
                If you did not make this payment, please contact us immediately.
            </p>
            """.formatted(amount));
    }
}
