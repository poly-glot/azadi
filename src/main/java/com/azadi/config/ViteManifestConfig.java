package com.azadi.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads the Vite manifest ({@code static/.vite/manifest.json}) at startup and
 * exposes resolved hashed asset paths to Thymeleaf templates.
 */
@ControllerAdvice
public class ViteManifestConfig {

    private final Map<String, String> manifest;

    public ViteManifestConfig() throws IOException {
        var resource = new ClassPathResource("static/.vite/manifest.json");
        if (resource.exists()) {
            var mapper = JsonMapper.builder().build();
            Map<String, Map<String, Object>> raw = mapper.readValue(
                resource.getInputStream(),
                new TypeReference<>() {}
            );
            manifest = new HashMap<>();
            for (var entry : raw.entrySet()) {
                manifest.put(entry.getKey(), "/" + entry.getValue().get("file"));
            }
        } else {
            manifest = Map.of();
        }
    }

    @ModelAttribute("viteAsset")
    public Map<String, String> viteAsset() {
        return manifest;
    }
}
