package com.azadi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Overrides the request host/scheme and intercepts redirects so that
 * Spring Security (and Tomcat) build redirect URLs using the configured
 * public domain instead of the internal Cloud Run host.
 */
@Configuration
public class AppDomainFilter {

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> appDomainFilterRegistration(
            @Value("${azadi.app-domain:}") String appDomain) {

        boolean active = isProductionDomain(appDomain);

        String scheme;
        String host;
        int port;
        String baseUrl;
        if (active) {
            scheme = "https";
            host = parseDomainHost(appDomain);
            port = parseDomainPort(appDomain);
            String portSuffix = (port == 443) ? "" : ":" + port;
            baseUrl = scheme + "://" + host + portSuffix;
        } else {
            scheme = "http";
            host = "localhost";
            port = 8080;
            baseUrl = "";
        }

        var filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                if (active) {
                    filterChain.doFilter(
                            new DomainOverrideRequest(request, host, port, scheme),
                            new DomainOverrideResponse(response, baseUrl, host));
                } else {
                    filterChain.doFilter(request, response);
                }
            }
        };

        var registration = new FilterRegistrationBean<OncePerRequestFilter>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    private static boolean isProductionDomain(String appDomain) {
        return appDomain != null && !appDomain.isBlank()
                && !appDomain.startsWith("localhost");
    }

    private static String parseDomainHost(String appDomain) {
        return appDomain.contains(":") ? appDomain.split(":", 2)[0] : appDomain;
    }

    private static int parseDomainPort(String appDomain) {
        return appDomain.contains(":") ? Integer.parseInt(appDomain.split(":", 2)[1]) : 443;
    }

    static class DomainOverrideRequest extends HttpServletRequestWrapper {
        private final String host;
        private final int port;
        private final String scheme;

        DomainOverrideRequest(HttpServletRequest request, String host, int port, String scheme) {
            super(request);
            this.host = host;
            this.port = port;
            this.scheme = scheme;
        }

        @Override
        public String getServerName() {
            return host;
        }

        @Override
        public int getServerPort() {
            return port;
        }

        @Override
        public String getScheme() {
            return scheme;
        }

        @Override
        public boolean isSecure() {
            return "https".equals(scheme);
        }

        @Override
        public StringBuffer getRequestURL() {
            var url = new StringBuffer(scheme).append("://").append(host);
            if (port != 443 && port != 80) {
                url.append(':').append(port);
            }
            url.append(getRequestURI());
            return url;
        }
    }

    static class DomainOverrideResponse extends HttpServletResponseWrapper {
        private final String baseUrl;
        private final String host;

        DomainOverrideResponse(HttpServletResponse response, String baseUrl, String host) {
            super(response);
            this.baseUrl = baseUrl;
            this.host = host;
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            if (location.startsWith("/")) {
                super.sendRedirect(baseUrl + location);
            } else if (location.startsWith("http://") || location.startsWith("https://")) {
                if (location.contains(host)) {
                    super.sendRedirect(location);
                } else {
                    var path = location.replaceFirst("https?://[^/]+", "");
                    super.sendRedirect(baseUrl + path);
                }
            } else {
                super.sendRedirect(baseUrl + "/" + location);
            }
        }
    }
}
