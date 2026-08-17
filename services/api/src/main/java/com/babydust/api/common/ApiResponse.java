package com.babydust.api.common;

import java.util.UUID;

public record ApiResponse<T>(String requestId, boolean success, T data, ApiError error) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(UUID.randomUUID().toString(), true, data, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(UUID.randomUUID().toString(), false, null, new ApiError(code, message));
    }
}
