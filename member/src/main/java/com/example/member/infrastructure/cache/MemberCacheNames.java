package com.example.member.infrastructure.cache;

import com.example.common.Constants;

public final class MemberCacheNames {

    private static final String SERVICE = Constants.SERVICE_NAME;
    private static final String RESOURCE = "MEMBER";

    public static final String MEMBER = SERVICE + "::" + RESOURCE;
    public static final String MEMBER_STATUS = SERVICE + "::" + RESOURCE + "::STATUS";

    private MemberCacheNames() {}
}
