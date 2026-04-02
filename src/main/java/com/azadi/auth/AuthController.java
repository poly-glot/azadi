package com.azadi.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    private final boolean demoMode;

    public AuthController(@Value("${azadi.seed-data:false}") boolean demoMode) {
        this.demoMode = demoMode;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("demoMode", demoMode);
        return "login";
    }

    @GetMapping("/login-error")
    public String loginError(Model model) {
        model.addAttribute("error", true);
        model.addAttribute("demoMode", demoMode);
        return "login";
    }
}
