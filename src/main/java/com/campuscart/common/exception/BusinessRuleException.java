package com.campuscart.common.exception;

/**
 * Thrown when an operation violates a domain/business invariant. Maps to HTTP 409.
 */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
}
