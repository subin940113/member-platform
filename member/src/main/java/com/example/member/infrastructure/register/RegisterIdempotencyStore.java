package com.example.member.infrastructure.register;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegisterIdempotencyStore {

    private static final String PROCESSING = "PROCESSING";

    private final StringRedisTemplate stringRedisTemplate;
    private final RegisterProperties registerProperties;

    public boolean acquire(String idempotencyKey) {
        String redisKey = RegisterRedisKeySpec.idempotencyKey(idempotencyKey.strip());
        var ttl = registerProperties.idempotency().ttl();
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, PROCESSING, ttl);
        return Boolean.TRUE.equals(first);
    }

    public void clear(String idempotencyKey) {
        stringRedisTemplate.delete(RegisterRedisKeySpec.idempotencyKey(idempotencyKey.strip()));
    }
}
