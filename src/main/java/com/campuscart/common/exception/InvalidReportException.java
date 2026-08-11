package com.campuscart.common.exception;

public class InvalidReportException extends ApiException {

    public InvalidReportException(String message) {
        super(ErrorCode.INVALID_REPORT, message);
    }
}
