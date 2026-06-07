package com.shike.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultDTO<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ResultDTO<T> success(T data) {
        return new ResultDTO<>(200, "success", data);
    }

    public static <T> ResultDTO<T> success() {
        return new ResultDTO<>(200, "success", null);
    }

    public static <T> ResultDTO<T> error(int code, String message) {
        return new ResultDTO<>(code, message, null);
    }

    public static <T> ResultDTO<T> error(String message) {
        return new ResultDTO<>(500, message, null);
    }
}
