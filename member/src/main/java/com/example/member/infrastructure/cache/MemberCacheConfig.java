package com.example.member.infrastructure.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableCaching
public class MemberCacheConfig implements CachingConfigurer {

    private final RedisConnectionFactory redisConnectionFactory;

    public MemberCacheConfig(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Bean
    @Override
    public CacheManager cacheManager() {
        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        for (MemberCacheSpec spec : MemberCacheSpec.values()) {
            perCache.put(
                    spec.cacheName(),
                    RedisCacheConfiguration.defaultCacheConfig()
                            .entryTtl(Duration.ofSeconds(spec.ttlSeconds())));
        }

        return RedisCacheManager.builder(redisConnectionFactory)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new MemberCacheErrorHandler();
    }
}
