package com.azadi.email.templates;

import org.springframework.web.util.HtmlUtils;

public final class LoginAlertTemplate {

    private LoginAlertTemplate() {
    }

    public static String build(String ipAddress) {
        var safeIp = HtmlUtils.htmlEscape(ipAddress);
        return EmailLayoutTemplate.wrap("Login Alert", """
            <h1 style="color:#0c121d;font-family:'Poppins',Arial,sans-serif;font-size:24px;margin:0 0 16px;">
                New Login Detected
            </h1>
            <p style="color:#0c121d;font-family:'Poppins',Arial,sans-serif;font-size:16px;line-height:1.5;">
                A new login to your Azadi Finance account was detected.
            </p>
            <table style="margin:16px 0;font-family:'Poppins',Arial,sans-serif;font-size:14px;">
                <tr><td style="padding:4px 12px 4px 0;color:#666;">IP Address:</td><td>%s</td></tr>
            </table>
            <p style="color:#666;font-family:'Poppins',Arial,sans-serif;font-size:14px;line-height:1.5;margin-top:24px;">
                If this was not you, please change your credentials and contact us immediately.
            </p>
            """.formatted(safeIp));
    }
}
