package com.azadi.email.templates;

public final class EmailLayoutTemplate {

    private EmailLayoutTemplate() {
    }

    public static String wrap(String title, String bodyContent) {
        return """
            <!DOCTYPE html>
            <html lang="en" xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <meta name="x-apple-disable-message-reformatting">
                <!--[if mso]>
                <noscript>
                    <xml>
                        <o:OfficeDocumentSettings>
                            <o:PixelsPerInch>96</o:PixelsPerInch>
                        </o:OfficeDocumentSettings>
                    </xml>
                </noscript>
                <![endif]-->
                <title>%s</title>
                <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@400;600&display=swap" rel="stylesheet">
            </head>
            <body style="margin:0;padding:0;background-color:#fcfbfc;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                       style="background-color:#fcfbfc;">
                    <tr>
                        <td align="center" style="padding:40px 20px;">
                            <table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0"
                                   style="max-width:600px;width:100%%;">
                                <tr>
                                    <td style="background-color:#0c121d;padding:24px 32px;text-align:center;">
                                        <h2 style="color:#ffffff;font-family:'Poppins',Arial,sans-serif;font-size:20px;margin:0;">
                                            Azadi Finance
                                        </h2>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="background-color:#ffffff;padding:32px;">
                                        %s
                                    </td>
                                </tr>
                                <tr>
                                    <td style="background-color:#f5f5f5;padding:24px 32px;text-align:center;">
                                        <p style="color:#999;font-family:'Poppins',Arial,sans-serif;font-size:12px;margin:0;">
                                            Azadi Finance Portal. This is an automated message, please do not reply directly.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(title, bodyContent);
    }
}
