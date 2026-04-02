package com.azadi.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LoginAttemptTracker {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);

    record LoginAttempt(AtomicInteger count, Instant lockUntil, Instant createdAt) {
        static LoginAttempt create() {
            return new LoginAttempt(new AtomicInteger(0), Instant.MIN, Instant.now());
        }
    }

    private final ConcurrentHashMap<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String agreementNumber) {
        var attempt = attempts.get(agreementNumber);
        if (attempt == null) {
            return false;
        }
        return attempt.count().get() >= MAX_ATTEMPTS && Instant.now().isBefore(attempt.lockUntil());
    }

    public void recordFailure(String agreementNumber) {
        var attempt = attempts.computeIfAbsent(agreementNumber, k -> LoginAttempt.create());
        var count = attempt.count().incrementAndGet();
        if (count >= MAX_ATTEMPTS) {
            attempts.put(agreementNumber,
                new LoginAttempt(new AtomicInteger(count), Instant.now().plus(LOCK_DURATION), attempt.createdAt()));
        }
    }

    public void recordSuccess(String agreementNumber) {
        attempts.remove(agreementNumber);
    }

    public void clearAll() {
        attempts.clear();
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanupExpiredEntries() {
        var now = Instant.now();
        attempts.entrySet().removeIf(entry -> {
            var attempt = entry.getValue();
            if (attempt.count().get() >= MAX_ATTEMPTS) {
                return now.isAfter(attempt.lockUntil());
            }
            return now.isAfter(attempt.createdAt().plus(LOCK_DURATION));
        });
    }
}
