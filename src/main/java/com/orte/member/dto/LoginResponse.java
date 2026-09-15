package com.orte.member.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
