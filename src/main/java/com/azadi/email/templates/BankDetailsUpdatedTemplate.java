package com.azadi.email.templates;

public final class BankDetailsUpdatedTemplate {

    private BankDetailsUpdatedTemplate() {
    }

    public static String build() {
        return EmailLayoutTemplate.wrap("Bank Details Updated", """
            <h1 style="color:#0c121d;font-family:'Poppins',Arial,sans-serif;font-size:24px;margin:0 0 16px;">
                Bank Details Updated
            </h1>
            <p style="color:#0c121d;font-family:'Poppins',Arial,sans-serif;font-size:16px;line-height:1.5;">
                Your bank details have been successfully updated on your account.
            </p>
            <p style="color:#0c121d;font-family:'Poppins',Arial,sans-serif;font-size:16px;line-height:1.5;">
                Future payments will be taken from your new bank account.
            </p>
            <p style="color:#666;font-family:'Poppins',Arial,sans-serif;font-size:14px;line-height:1.5;margin-top:24px;">
                If you did not make this change, please contact us immediately.
            </p>
            """);
    }
}
