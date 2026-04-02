package com.azadi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    record RateBucket(AtomicInteger count, Instant resetAt) {
        static RateBucket create(Duration window) {
            return new RateBucket(new AtomicInteger(0), Instant.now().plus(window));
        }

        boolean isExpired() {
            return Instant.now().isAfter(resetAt);
        }
    }

    private static final int LOGIN_MAX = 5;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    private static final int PAYMENT_MAX = 3;
    private static final Duration PAYMENT_WINDOW = Duration.ofHours(1);
    private static final int GENERAL_MAX = 60;
    private static final Duration GENERAL_WINDOW = Duration.ofMinutes(1);

    private final ConcurrentHashMap<String, RateBucket> loginBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RateBucket> paymentBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RateBucket> generalBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var path = request.getRequestURI();
        var method = request.getMethod();

        if ("POST".equals(method) && "/login".equals(path)) {
            var ip = extractClientIp(request);
            if (isRateLimited(loginBuckets, "login:" + ip, LOGIN_MAX, LOGIN_WINDOW)) {
                sendTooManyRequests(response, LOGIN_WINDOW);
                return;
            }
        }

        if ("POST".equals(method) && path.contains("/make-a-payment")) {
            var sessionId = request.getSession(false) != null ? request.getSession().getId() : extractClientIp(request);
            if (isRateLimited(paymentBuckets, "pay:" + sessionId, PAYMENT_MAX, PAYMENT_WINDOW)) {
                sendTooManyRequests(response, PAYMENT_WINDOW);
                return;
            }
        }

        var sessionKey = request.getSession(false) != null ? request.getSession().getId() : extractClientIp(request);
        if (isRateLimited(generalBuckets, "gen:" + sessionKey, GENERAL_MAX, GENERAL_WINDOW)) {
            sendTooManyRequests(response, GENERAL_WINDOW);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(ConcurrentHashMap<String, RateBucket> buckets,
                                  String key, int max, Duration window) {
        var bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return RateBucket.create(window);
            }
            return existing;
        });
        return bucket.count().incrementAndGet() > max;
    }

    private void sendTooManyRequests(HttpServletResponse response, Duration retryAfter) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfter.toSeconds()));
        response.getWriter().write("Too many requests. Please try again later.");
    }

    private String extractClientIp(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanupExpiredBuckets() {
        loginBuckets.entrySet().removeIf(entry -> entry.getValue().isExpired());
        paymentBuckets.entrySet().removeIf(entry -> entry.getValue().isExpired());
        generalBuckets.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public void clearAll() {
        loginBuckets.clear();
        paymentBuckets.clear();
        generalBuckets.clear();
    }
}
