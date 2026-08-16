package com.chessfraud.gateway.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Fixed-window rate limiter backed by Redis. A per-pod in-memory limiter would let the
 * effective limit scale with the number of gateway replicas (each pod enforcing its own
 * counter) instead of the intended global limit - this keeps a single shared counter per
 * key regardless of which pod serves the request.
 */
@Component
public class RateLimiter {
    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate redis;

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Returns true if a call under {@code key} is allowed within the current window. */
    public boolean tryAcquire(String key, int limit, Duration window) {
        String redisKey = KEY_PREFIX + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redis.expire(redisKey, window);
        }
        return count != null && count <= limit;
    }

    public void removeSession(String key) {
        redis.delete(KEY_PREFIX + key);
    }
}
