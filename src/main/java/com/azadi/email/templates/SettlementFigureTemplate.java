package com.azadi.email.templates;

public final class SettlementFigureTemplate {

    private SettlementFigureTemplate() {
    }

    public static String build(long amountPence, String validUntil) {
        var amount = String.format("\u00A3%,.2f", amountPence / 100.0);
        return EmailLayoutTemplate.wrap("Settlement Figure", """
            <h1 style="color:#0c121d;font-family:'Poppins',Arial,sans-serif;font-size:24px;margin:0 0 16px;">
                Your Settlement Figure
            </h1>
            <p style="color:#0c121d;font-family:'Poppins',Arial,sans-serif;font-size:16px;line-height:1.5;">
                Your settlement figure is <strong>%s</strong>.
            </p>
            <p style="color:#0c121d;font-family:'Poppins',Arial,sans-serif;font-size:16px;line-height:1.5;">
                This figure is valid until <strong>%s</strong>.
            </p>
            <p style="color:#666;font-family:'Poppins',Arial,sans-serif;font-size:14px;line-height:1.5;margin-top:24px;">
                To proceed with settlement, please contact our team or make a payment through your online account.
            </p>
            """.formatted(amount, validUntil));
    }
}
