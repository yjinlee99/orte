package com.orte.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
