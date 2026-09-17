package com.orte.auth.exception;

import com.orte.exception.BusinessException;
import com.orte.exception.ErrorCode;

public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS);
    }
}