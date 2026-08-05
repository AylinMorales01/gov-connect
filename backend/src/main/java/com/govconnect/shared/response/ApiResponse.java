package com.govconnect.shared.response;

import java.time.LocalDateTime;

public record ApiResponse<T>(
        boolean success,
        String message,
        LocalDateTime timestamp,
        T data
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                true,
                message,
                LocalDateTime.now(),
                data
        );
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(
                false,
                message,
                LocalDateTime.now(),
                null
        );
    }
}