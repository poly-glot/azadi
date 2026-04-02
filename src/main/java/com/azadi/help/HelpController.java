package com.azadi.help;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelpController {

    @GetMapping("/help/faqs")
    public String faqs() {
        return "help/faqs";
    }

    @GetMapping("/help/ways-to-pay")
    public String waysToPay() {
        return "help/ways-to-pay";
    }

    @GetMapping("/help/contact-us")
    public String contactUs() {
        return "help/contact-us";
    }

    @GetMapping("/cookies")
    public String cookies() {
        return "legal/cookies";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "legal/privacy";
    }

    @GetMapping("/terms")
    public String terms() {
        return "legal/terms";
    }
}
