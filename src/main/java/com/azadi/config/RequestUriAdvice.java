package com.azadi.config;

import com.azadi.auth.AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
public class RequestUriAdvice {

    private final AuthorizationService authorizationService;
    private final String appVersion;

    public RequestUriAdvice(AuthorizationService authorizationService,
                            Optional<BuildProperties> buildProperties,
                            Optional<GitProperties> gitProperties) {
        this.authorizationService = authorizationService;
        var version = buildProperties.map(BuildProperties::getVersion).orElse("dev");
        var commit = gitProperties.map(GitProperties::getShortCommitId).orElse("local");
        this.appVersion = version + "-" + commit;
    }

    @ModelAttribute("requestURI")
    public String requestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("appVersion")
    public String appVersion() {
        return appVersion;
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
