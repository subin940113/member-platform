package com.example.member.infrastructure.register;

import com.example.common.Constants;

public final class RegisterRedisKeySpec {

    private static final String SERVICE = Constants.SERVICE_NAME;
    private static final String RESOURCE = "MEMBER";
    private static final String REGISTER = "REGISTER";
    private static final String ADMISSION = "ADMISSION";
    private static final String TOKEN = "TOKEN";
    private static final String SLOT = "SLOT";
    private static final String IDEMPOTENCY = "IDEMPOTENCY";

    private RegisterRedisKeySpec() {}

    public static String admissionTokenKey(String admissionToken) {
        return SERVICE + "::" + RESOURCE + "::" + REGISTER + "::" + ADMISSION + "::" + TOKEN + "::" + admissionToken;
    }

    public static String admissionSlotKey(int slotNumber) {
        return SERVICE + "::" + RESOURCE + "::" + REGISTER + "::" + ADMISSION + "::" + SLOT + "::" + slotNumber;
    }

    public static String idempotencyKey(String idempotencyKey) {
        return SERVICE + "::" + RESOURCE + "::" + REGISTER + "::" + IDEMPOTENCY + "::" + idempotencyKey;
    }
}
