package com.example.member.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

public class MemberCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(MemberCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn(
                "[member-cache] cache get failed, using uncached path. cacheName={}, cacheKey={}",
                cache.getName(),
                key,
                exception);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn(
                "[member-cache] cache put failed, database remains source of truth. cacheName={}, cacheKey={}",
                cache.getName(),
                key,
                exception);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn(
                "[member-cache] cache evict failed. cacheName={}, cacheKey={}",
                cache.getName(),
                key,
                exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("[member-cache] cache clear failed. cacheName={}", cache.getName(), exception);
    }
}
