package com.azadi.help;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/help")
public class HelpController {

    @GetMapping("/faqs")
    public String faqs() {
        return "help/faqs";
    }

    @GetMapping("/ways-to-pay")
    public String waysToPay() {
        return "help/ways-to-pay";
    }

    @GetMapping("/contact-us")
    public String contactUs() {
        return "help/contact-us";
    }
}
