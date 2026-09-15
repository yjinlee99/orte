package com.orte.member.exception;

import com.orte.exception.BusinessException;
import com.orte.exception.ErrorCode;

public class NicknameAlreadyExistsException extends BusinessException {

    public NicknameAlreadyExistsException() {
        super(ErrorCode.NICKNAME_ALREADY_EXISTS);
    }
}