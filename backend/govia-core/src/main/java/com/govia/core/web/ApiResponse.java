package com.govia.core.web;

import java.time.Instant;

/**
 * Response wrapper chuan cho toan bo API cua platform, giup frontend
 * (govia-ui-kit) xu ly thanh cong/loi theo 1 format duy nhat.
 */
public record ApiResponse<T>(boolean success, T data, String errorCode, String message, Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, null, errorCode, message, Instant.now());
    }
}
