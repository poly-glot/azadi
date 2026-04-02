package com.azadi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CspNonceFilter extends OncePerRequestFilter {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int NONCE_BYTE_LENGTH = 16;

    private final String viteDevUrl;

    public CspNonceFilter(@Value("${azadi.vite-dev-url:}") String viteDevUrl) {
        this.viteDevUrl = viteDevUrl.isBlank() ? null : viteDevUrl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var nonceBytes = new byte[NONCE_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(nonceBytes);
        var nonce = Base64.getEncoder().encodeToString(nonceBytes);

        request.setAttribute("cspNonce", nonce);

        // In dev mode, allow Vite dev server as script/style/connect source
        var viteOrigin = viteDevUrl != null ? " " + viteDevUrl : "";
        var viteWs = viteDevUrl != null
            ? " " + viteDevUrl.replace("http://", "ws://")
            : "";

        var csp = String.join("",
            "default-src 'self'; ",
            "script-src 'self' 'nonce-", nonce, "' https://js.stripe.com", viteOrigin, "; ",
            "style-src 'self' 'nonce-", nonce, "' https://fonts.googleapis.com", viteOrigin, "; ",
            "font-src 'self' https://fonts.gstatic.com; ",
            "frame-src https://js.stripe.com; ",
            "img-src 'self' data:", viteOrigin, "; ",
            "connect-src 'self' https://api.stripe.com", viteOrigin, viteWs, "; ",
            "base-uri 'self'; ",
            "form-action 'self'; ",
            "report-uri /api/csp-report;"
        );

        response.setHeader("Content-Security-Policy", csp);
        filterChain.doFilter(request, response);
    }
}
