package com.example.common.exception;

import org.springframework.dao.DataIntegrityViolationException;

public final class DataIntegrityViolations {

    private DataIntegrityViolations() {}

    public static BusinessException toBusinessException(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message != null && message.contains("uk_members_email")) {
            return new BusinessException(ErrorCode.MEMBER_DUPLICATE_EMAIL);
        }
        if (message != null && message.contains("uk_members_phone_hash")) {
            return new BusinessException(ErrorCode.MEMBER_DUPLICATE_PHONE);
        }
        return new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    public static DataIntegrityViolationException findDataIntegrity(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof DataIntegrityViolationException dive) {
                return dive;
            }
            t = t.getCause();
        }
        return null;
    }
}
