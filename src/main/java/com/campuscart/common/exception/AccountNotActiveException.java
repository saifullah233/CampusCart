package com.campuscart.common.exception;

public class AccountNotActiveException extends ApiException {

    public AccountNotActiveException() {
        super(ErrorCode.ACCOUNT_NOT_ACTIVE, "The account is not active.");
    }
}
