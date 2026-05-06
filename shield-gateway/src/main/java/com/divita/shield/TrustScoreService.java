package com.divita.shield;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TrustScoreService {

    private final StringRedisTemplate redisTemplate;

    private static final int DEFAULT_SCORE = 50;
    private static final int MAX_SCORE = 100;
    private static final int MIN_SCORE = 0;

    public TrustScoreService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public int getScore(String clientId) {
        String key = getKey(clientId);
        String value = redisTemplate.opsForValue().get(key);

        if(value == null) {
            redisTemplate.opsForValue().set(key, String.valueOf(DEFAULT_SCORE));
            return DEFAULT_SCORE;
        }

        return Integer.parseInt(value);
    }

    public int increaseScore(String clientId, int amount) {
        int current = getScore(clientId);
        int updated = Math.min(MAX_SCORE, current + amount);
        redisTemplate.opsForValue().set(getKey(clientId), String.valueOf(updated));
        return updated;
    }

    public int decreaseScore(String clientId, int amount) {
        int current = getScore(clientId);
        int updated = Math.max(MIN_SCORE, current - amount);
        redisTemplate.opsForValue().set(getKey(clientId), String.valueOf(updated));
        return updated;
    }

    public boolean isBlocked(String clientId) {
        Boolean exists = redisTemplate.hasKey("block:" + clientId);
        return Boolean.TRUE.equals(exists);
    }

    private String getKey(String clientId) {
        return "trust:" + clientId;
    }

    public void blockClient(String clientId, Duration duration) {
        String key = "block:" + clientId;
        redisTemplate.opsForValue().set(key, "BLOCKED", duration);
    }

    public Duration getBlockDuration(int offenses) {
        return switch (offenses) {
            case 1 -> Duration.ofMinutes(2);
            case 2 -> Duration.ofMinutes(5);
            case 3 -> Duration.ofMinutes(15);
            case 4 -> Duration.ofHours(1);
            default -> Duration.ofHours(24);
        };
    }

    public void applyProgressiveBlock(String clientId) {
        String offenseKey = "offense:" + clientId;

        Long offenses = redisTemplate.opsForValue().increment(offenseKey);

        if (offenses != null && offenses == 1) {
            redisTemplate.expire(offenseKey, Duration.ofDays(1));
        }

        Duration duration = getBlockDuration(offenses == null ? 1 : offenses.intValue());

        blockClient(clientId, duration);
    }

    public int getOffenseCount(String clientId) {
        String value = redisTemplate.opsForValue().get("offense:" + clientId);
        return value == null ? 0 : Integer.parseInt(value);
    }
}
