package com.azadi.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

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
            manifest = raw.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                    Map.Entry::getKey,
                    entry -> "/" + entry.getValue().get("file")));
        } else {
            manifest = Map.of();
        }
    }

    @ModelAttribute("viteAsset")
    public Map<String, String> viteAsset() {
        return manifest;
    }
}
