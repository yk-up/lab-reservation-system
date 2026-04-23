package com.lab.reservation.vo;

import com.lab.reservation.exception.ErrorCode;
import lombok.Data;

/**
 * 统一响应结果封装
 */
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getDefaultMessage(), data);
    }

    public static <T> Result<T> success() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getDefaultMessage(), null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(ErrorCode.PARAM_INVALID.getCode(), message, null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getDefaultMessage(), null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    public static <T> Result<T> unauthorized() {
        return new Result<>(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getDefaultMessage(), null);
    }

    public static <T> Result<T> forbidden() {
        return new Result<>(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getDefaultMessage(), null);
    }
}
