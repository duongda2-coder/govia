package com.govia.core.web;

import org.springframework.http.HttpStatus;

/**
 * Loi nghiep vu du kien truoc (vd: trung ma nhan vien, khong du quyen theo ABAC scope...).
 * Moi module nen throw exception nay thay vi RuntimeException chung chung,
 * de GlobalExceptionHandler tra ve dung http status + errorCode cho frontend.
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public BusinessException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
