package com.azadi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Injects the Vite dev server URL into all Thymeleaf templates.
 *
 * <p>When the {@code dev} profile is active, {@code azadi.vite-dev-url} points to the
 * local Vite server (e.g. {@code http://localhost:5173}). Thymeleaf uses this to load
 * CSS/JS via HMR instead of the hashed production assets.</p>
 *
 * <p>In production the property is empty, so templates fall back to the built assets
 * in {@code /assets/}.</p>
 */
@ControllerAdvice
public class ViteDevConfig {

    private final String viteDevUrl;

    public ViteDevConfig(@Value("${azadi.vite-dev-url:}") String viteDevUrl) {
        this.viteDevUrl = viteDevUrl.isBlank() ? "" : viteDevUrl;
    }

    @ModelAttribute("viteDevUrl")
    public String viteDevUrl() {
        return viteDevUrl.isEmpty() ? null : viteDevUrl;
    }
}
