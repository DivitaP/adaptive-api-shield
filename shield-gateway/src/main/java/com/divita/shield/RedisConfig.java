package com.divita.shield;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public RateLimiterService rateLimiterService(StringRedisTemplate redisTemplate) {
        return new RateLimiterService(redisTemplate);
    }
}
