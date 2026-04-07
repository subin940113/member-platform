package com.example.member.infrastructure.cache;

public enum MemberCacheSpec {

    MEMBER(MemberCacheNames.MEMBER, 1800L),
    MEMBER_STATUS(MemberCacheNames.MEMBER_STATUS, 86400L);

    private final String cacheName;

    private final long ttlSeconds;

    MemberCacheSpec(String cacheName, long ttlSeconds) {
        this.cacheName = cacheName;
        this.ttlSeconds = ttlSeconds;
    }

    public String cacheName() {
        return cacheName;
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }
}
