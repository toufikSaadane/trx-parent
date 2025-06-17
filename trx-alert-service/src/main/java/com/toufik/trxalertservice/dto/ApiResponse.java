package com.toufik.trxalertservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    @JsonProperty("data")
    private T data;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, LocalDateTime.now(), "success", null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(data, LocalDateTime.now(), "success", message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(null, LocalDateTime.now(), "error", message);
    }
}
