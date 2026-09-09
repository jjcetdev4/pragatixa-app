package jjcet.PragatiX.modules.authentication.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe rate limiter and brute-force protection service for OTP operations.
 */
@Service
public class OtpRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(OtpRateLimiterService.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_SECONDS = 15 * 60; // 15 minutes
    private static final long REQUEST_COOLDOWN_SECONDS = 30; // 30 seconds between requests

    private static class AttemptRecord {
        int failureCount = 0;
        long lockedUntilEpochSecond = 0;
        long lastRequestEpochSecond = 0;
    }

    private final ConcurrentHashMap<String, AttemptRecord> emailAttempts = new ConcurrentHashMap<>();

    /**
     * Checks if the email is currently locked out due to excessive failed attempts.
     */
    public boolean isLockedOut(String email) {
        if (email == null) return false;
        String key = email.trim().toLowerCase();
        AttemptRecord record = emailAttempts.get(key);
        if (record == null) return false;

        long now = Instant.now().getEpochSecond();
        if (record.lockedUntilEpochSecond > now) {
            return true;
        }

        // If lockout expired, reset
        if (record.lockedUntilEpochSecond > 0 && record.lockedUntilEpochSecond <= now) {
            record.failureCount = 0;
            record.lockedUntilEpochSecond = 0;
        }
        return false;
    }

    /**
     * Returns remaining lockout seconds if locked out, or 0.
     */
    public long getRemainingLockoutMinutes(String email) {
        if (email == null) return 0;
        String key = email.trim().toLowerCase();
        AttemptRecord record = emailAttempts.get(key);
        if (record == null) return 0;

        long now = Instant.now().getEpochSecond();
        long diff = record.lockedUntilEpochSecond - now;
        if (diff <= 0) return 0;
        return (diff + 59) / 60; // Round up to nearest minute
    }

    /**
     * Checks if an OTP request was made too recently (cooldown protection).
     */
    public boolean isRequestOnCooldown(String email) {
        if (email == null) return false;
        String key = email.trim().toLowerCase();
        AttemptRecord record = emailAttempts.get(key);
        if (record == null) return false;

        long now = Instant.now().getEpochSecond();
        return (now - record.lastRequestEpochSecond) < REQUEST_COOLDOWN_SECONDS;
    }

    /**
     * Returns remaining cooldown seconds before a new OTP can be requested.
     */
    public long getRemainingCooldownSeconds(String email) {
        if (email == null) return 0;
        String key = email.trim().toLowerCase();
        AttemptRecord record = emailAttempts.get(key);
        if (record == null) return 0;

        long now = Instant.now().getEpochSecond();
        long elapsed = now - record.lastRequestEpochSecond;
        return Math.max(0, REQUEST_COOLDOWN_SECONDS - elapsed);
    }

    /**
     * Records a new OTP dispatch timestamp for cooldown enforcement.
     */
    public void recordOtpRequested(String email) {
        if (email == null) return;
        String key = email.trim().toLowerCase();
        emailAttempts.compute(key, (k, v) -> {
            if (v == null) v = new AttemptRecord();
            v.lastRequestEpochSecond = Instant.now().getEpochSecond();
            return v;
        });
    }

    /**
     * Records a failed OTP verification attempt. Triggers a 15-minute lockout if threshold reached.
     */
    public void recordFailedAttempt(String email) {
        if (email == null) return;
        String key = email.trim().toLowerCase();
        emailAttempts.compute(key, (k, v) -> {
            if (v == null) v = new AttemptRecord();
            v.failureCount++;
            if (v.failureCount >= MAX_FAILED_ATTEMPTS) {
                v.lockedUntilEpochSecond = Instant.now().getEpochSecond() + LOCKOUT_DURATION_SECONDS;
                log.warn("[SECURITY ALERT] Excessive failed OTP attempts for email '{}'. Account locked for 15 minutes.", key);
            }
            return v;
        });
    }

    /**
     * Clears all failed attempts and lockouts upon successful verification.
     */
    public void clearAttempts(String email) {
        if (email == null) return;
        emailAttempts.remove(email.trim().toLowerCase());
    }
}
