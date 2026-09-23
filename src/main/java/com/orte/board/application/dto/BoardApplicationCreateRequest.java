package com.orte.board.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardApplicationCreateRequest(

        @NotBlank(message = "게시판 제목은 필수입니다.")
        @Size(max = 100, message = "게시판 제목은 100자 이하로 입력해 주세요.")
        String title,

        @NotBlank(message = "게시판 소개는 필수입니다.")
        String description
) {
}