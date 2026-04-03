package com.azadi.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptTracker {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);

    record LoginAttempt(int count, Instant lockUntil, Instant createdAt) {
        static LoginAttempt create() {
            return new LoginAttempt(0, Instant.MIN, Instant.now());
        }

        LoginAttempt incrementAndLockIfNeeded(Duration lockDuration, int maxAttempts) {
            var newCount = count + 1;
            var newLockUntil = newCount >= maxAttempts ? Instant.now().plus(lockDuration) : lockUntil;
            return new LoginAttempt(newCount, newLockUntil, createdAt);
        }
    }

    private final ConcurrentHashMap<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String agreementNumber) {
        var attempt = attempts.get(agreementNumber);
        if (attempt == null) {
            return false;
        }
        return attempt.count() >= MAX_ATTEMPTS && Instant.now().isBefore(attempt.lockUntil());
    }

    public void recordFailure(String agreementNumber) {
        attempts.compute(agreementNumber, (k, existing) -> {
            var attempt = existing != null ? existing : LoginAttempt.create();
            return attempt.incrementAndLockIfNeeded(LOCK_DURATION, MAX_ATTEMPTS);
        });
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
            if (attempt.count() >= MAX_ATTEMPTS) {
                return now.isAfter(attempt.lockUntil());
            }
            return now.isAfter(attempt.createdAt().plus(LOCK_DURATION));
        });
    }
}
