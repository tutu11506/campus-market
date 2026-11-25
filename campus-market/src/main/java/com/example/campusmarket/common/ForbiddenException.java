package com.example.campusmarket.common;

/**
 * 越权访问异常：请求合法但当前用户无权操作该资源。
 * 由 GlobalExceptionHandler 统一转换为 HTTP 403，便于前端与调用方区分
 * “业务失败（400）”与“无权限（403）”。
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
