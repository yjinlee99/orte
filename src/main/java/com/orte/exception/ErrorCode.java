package com.orte.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Common
    VALIDATION_FAILED(
            HttpStatus.BAD_REQUEST,
            "입력값을 확인해 주세요."
    ),

    INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "요청 본문을 확인해 주세요."
    ),

    UNSUPPORTED_MEDIA_TYPE(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "지원하지 않는 Content-Type입니다."
    ),

    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "서버 내부 오류가 발생했습니다."
    ),

    // Member
    EMAIL_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "이미 사용 중인 이메일입니다."
    ),

    NICKNAME_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "이미 사용 중인 닉네임입니다."
    ),

    INVALID_CREDENTIALS(
            HttpStatus.UNAUTHORIZED,
            "이메일 또는 비밀번호가 올바르지 않습니다."
    ),

    AUTHENTICATION_REQUIRED(
            HttpStatus.UNAUTHORIZED,
            "인증이 필요합니다."
    ),

    ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "접근 권한이 없습니다."
    ),

    // Board Application
    BOARD_APPLICATION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "게시판 개설 신청을 찾을 수 없습니다."
    ),

    BOARD_APPLICATION_ALREADY_PROCESSED(
            HttpStatus.CONFLICT,
            "이미 처리된 게시판 개설 신청입니다."
    ),

    // Refresh Token
    INVALID_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "유효하지 않은 Refresh Token입니다."
    );

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getCode() {
        return name();
    }
}