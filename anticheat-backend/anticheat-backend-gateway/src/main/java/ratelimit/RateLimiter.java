package ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private final int maxTokens;
    private final long refillIntervalNanos;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiter(int maxPerSecond) {
        this.maxTokens = maxPerSecond;
        this.refillIntervalNanos = 1_000_000_000L / maxPerSecond;
    }

    public boolean tryAcquire(String key) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(maxTokens));
        return bucket.tryConsume();
    }

    public void removeSession(String key) {
        buckets.remove(key);
    }

    private class TokenBucket {
        private double tokens;
        private long lastRefillNanos;

        TokenBucket(int maxTokens) {
            this.tokens = maxTokens;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            double newTokens = (double) elapsed / refillIntervalNanos;
            tokens = Math.min(maxTokens, tokens + newTokens);
            lastRefillNanos = now;
        }
    }
}
