package com.divita.shield;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;

public class RateLimiterService {
    private final StringRedisTemplate redisTemplate;

    private static final int DEFAULT_RATE_LIMIT = 3;
    private static final Duration WINDOW = Duration.ofSeconds(300);

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private int getLimitForEndpoint(String endpoint) {
        switch (endpoint) {
            case "/login":
                return 3;
            case "/order":
                return 5;
            case "/products":
                return 10;
            default:
                return 5;
        }
    }

    public boolean isAllowed(String clientId, String endpoint) {
        long currentWindow = Instant.now().getEpochSecond() / WINDOW.getSeconds();

        String key = "rate:" + clientId + ":" + endpoint + ":" + currentWindow;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW);
        }

        int limit = getLimitForEndpoint(endpoint);
        System.out.println("RATE_LIMIT_DEBUG endpoint=" + endpoint + ", count=" + count + ", limit=" + limit);
        return count != null && count <= limit;
    }
}
