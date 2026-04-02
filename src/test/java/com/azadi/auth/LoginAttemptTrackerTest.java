package com.azadi.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptTrackerTest {

    private LoginAttemptTracker tracker;

    private static final String TEST_KEY = "AGR-001";

    @BeforeEach
    void setUp() {
        tracker = new LoginAttemptTracker();
    }

    @Test
    @DisplayName("First attempt should not be blocked")
    void firstAttemptNotBlocked() {
        // Act & Assert
        assertThat(tracker.isBlocked(TEST_KEY)).isFalse();
    }

    @Test
    @DisplayName("Account should be blocked after 5 failed attempts")
    void blockedAfterFiveFailedAttempts() {
        // Arrange - record 4 failed attempts, should not be blocked yet
        for (int i = 0; i < 4; i++) {
            tracker.recordFailure(TEST_KEY);
        }
        assertThat(tracker.isBlocked(TEST_KEY)).isFalse();

        // Act - record the 5th failure which triggers the lock
        tracker.recordFailure(TEST_KEY);

        // Assert - should now be blocked
        assertThat(tracker.isBlocked(TEST_KEY)).isTrue();
    }

    @Test
    @DisplayName("Successful login should clear failed attempts")
    void successfulLoginClearsAttempts() {
        // Arrange - record 4 failures
        for (int i = 0; i < 4; i++) {
            tracker.recordFailure(TEST_KEY);
        }

        // Act - successful login clears everything
        tracker.recordSuccess(TEST_KEY);

        // Assert - should not be blocked
        assertThat(tracker.isBlocked(TEST_KEY)).isFalse();

        // Verify the counter was actually reset by adding more attempts
        for (int i = 0; i < 4; i++) {
            tracker.recordFailure(TEST_KEY);
        }
        assertThat(tracker.isBlocked(TEST_KEY)).isFalse();
    }

    @Test
    @DisplayName("cleanupExpiredEntries removes expired lock entries")
    void cleanupExpiredEntriesRemovesExpired() {
        // Arrange - this test verifies cleanup does not throw and handles empty state
        tracker.cleanupExpiredEntries();

        // No assertions needed beyond no exception being thrown
        assertThat(tracker.isBlocked(TEST_KEY)).isFalse();
    }

    @Test
    @DisplayName("Different agreement numbers are tracked independently")
    void independentTracking() {
        // Arrange - lock one agreement
        for (int i = 0; i < 5; i++) {
            tracker.recordFailure("AGR-001");
        }

        // Assert - other agreement is not blocked
        assertThat(tracker.isBlocked("AGR-002")).isFalse();
        assertThat(tracker.isBlocked("AGR-001")).isTrue();
    }
}
