package com.chessfraud.gateway.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory store for password-reset codes issued by /auth/forgot-password. */
@Component
public class PasswordResetCodeStore {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetCodeStore.class);
    private static final long TTL_SECONDS = 600;

    private final Map<String, ResetCode> pendingResetCodes = new ConcurrentHashMap<>();

    private record ResetCode(String code, Instant expiresAt) {
        boolean isValid(String inputCode) {
            return code.equals(inputCode) && Instant.now().isBefore(expiresAt);
        }
    }

    public void store(String email, String code) {
        if (email == null || email.isBlank() || code == null || code.isBlank()) {
            return;
        }
        Instant expiry = Instant.now().plusSeconds(TTL_SECONDS);
        pendingResetCodes.put(email.toLowerCase(), new ResetCode(code, expiry));
        log.info("[AUTH] Reset code stored for email '{}', expires at {}", email, expiry);
    }

    /** Validates the code for the given email and consumes it (valid or not). */
    public boolean validateAndConsume(String email, String code) {
        ResetCode stored = pendingResetCodes.remove(email.toLowerCase());
        return stored != null && stored.isValid(code);
    }

    @Scheduled(fixedRate = 5, initialDelay = 5, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    void cleanExpiredCodes() {
        pendingResetCodes.entrySet().removeIf(entry -> Instant.now().isAfter(entry.getValue().expiresAt()));
    }
}
