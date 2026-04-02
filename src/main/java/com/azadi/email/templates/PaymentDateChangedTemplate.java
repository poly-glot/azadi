package com.azadi.email.templates;

public final class PaymentDateChangedTemplate {

    private PaymentDateChangedTemplate() {
    }

    public static String build(String newDate) {
        return EmailLayoutTemplate.wrap("Payment Date Changed", """
            <h1 style="color:#0c121d;font-family:'Poppins',Arial,sans-serif;font-size:24px;margin:0 0 16px;">
                Payment Date Changed
            </h1>
            <p style="color:#0c121d;font-family:'Poppins',Arial,sans-serif;font-size:16px;line-height:1.5;">
                Your monthly payment date has been changed to <strong>%s</strong>.
            </p>
            <p style="color:#0c121d;font-family:'Poppins',Arial,sans-serif;font-size:16px;line-height:1.5;">
                All future direct debit payments will be collected on this date.
            </p>
            <p style="color:#666;font-family:'Poppins',Arial,sans-serif;font-size:14px;line-height:1.5;margin-top:24px;">
                If you did not request this change, please contact us immediately.
            </p>
            """.formatted(newDate));
    }
}
