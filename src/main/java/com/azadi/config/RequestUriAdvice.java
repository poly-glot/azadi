package com.azadi.config;

import com.azadi.auth.AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class RequestUriAdvice {

    private final AuthorizationService authorizationService;

    public RequestUriAdvice(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @ModelAttribute("requestURI")
    public String requestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("customerName")
    public String customerName() {
        try {
            return authorizationService.getCurrentCustomerName();
        } catch (IllegalStateException e) {
            return "Customer";
        }
    }
}
