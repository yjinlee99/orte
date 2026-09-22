package com.orte.board.application.dto;

import jakarta.validation.constraints.NotBlank;

public record BoardApplicationCreateRequest(

        @NotBlank(message = "게시판 제목은 필수입니다.")
        String title,

        @NotBlank(message = "게시판 소개는 필수입니다.")
        String description
) {
}