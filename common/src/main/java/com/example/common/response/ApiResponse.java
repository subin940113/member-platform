package com.example.common.response;

import com.example.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String code, String message, T data, String traceId) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", null, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, "OK", null, null, null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String traceId) {
        return new ApiResponse<>(
                false, errorCode.name(), errorCode.getDefaultMessage(), null, traceId);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message, String traceId) {
        return new ApiResponse<>(false, errorCode.name(), message, null, traceId);
    }

    public static <T> ApiResponse<T> failWithData(ErrorCode errorCode, String message, T data, String traceId) {
        return new ApiResponse<>(false, errorCode.name(), message, data, traceId);
    }

    public static ApiResponse<Void> fail(String code, String message, String traceId) {
        return new ApiResponse<>(false, code, message, null, traceId);
    }

    public static String newTraceId() {
        return UUID.randomUUID().toString();
    }
}
