package com.azadi.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class AuditService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditService.class);

    private final AuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditRepository auditRepository, ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    public void logEvent(String customerId, String eventType, String ipAddress,
                         Map<String, String> details) {
        CompletableFuture.runAsync(() -> {
            try {
                var event = new AuditEvent();
                event.setCustomerId(customerId);
                event.setEventType(eventType);
                event.setTimestamp(Instant.now());
                event.setIpAddress(ipAddress);
                event.setDetails(serializeDetails(details));
                event.setSessionIdHash(hashSessionId());
                auditRepository.save(event);
            } catch (Exception e) {
                LOG.error("Failed to write audit event: {} for customer: {}", eventType, customerId, e);
            }
        });
    }

    private String serializeDetails(Map<String, String> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to serialize audit details", e);
            return "{}";
        }
    }

    private String hashSessionId() {
        try {
            var requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes servletAttrs) {
                HttpServletRequest request = servletAttrs.getRequest();
                var session = request.getSession(false);
                if (session != null) {
                    var digest = MessageDigest.getInstance("SHA-256");
                    var hash = digest.digest(session.getId().getBytes(StandardCharsets.UTF_8));
                    return HexFormat.of().formatHex(hash);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            LOG.error("SHA-256 algorithm not available", e);
        }
        return "no-session";
    }
}
