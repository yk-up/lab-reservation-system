package com.lab.reservation.exception;

public enum ErrorCode {
    SUCCESS(200, "操作成功"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "权限不足"),
    INTERNAL_ERROR(500, "服务器内部错误，请稍后重试"),

    PARAM_INVALID(40001, "请求参数错误"),
    PAGE_PARAM_INVALID(40002, "分页参数错误"),
    DATA_NOT_FOUND(40401, "数据不存在"),
    BUSINESS_CONFLICT(40901, "业务冲突");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
