package com.orte.board.application.dto;

import com.orte.board.application.entity.BoardApplication;
import com.orte.board.application.entity.BoardApplicationStatus;

public record BoardApplicationResponse(
        Long id,
        Long applicantId,
        String title,
        String description,
        String imagePath,
        BoardApplicationStatus status
) {

    public static BoardApplicationResponse from(BoardApplication application) {
        return new BoardApplicationResponse(
                application.getId(),
                application.getApplicantId(),
                application.getTitle(),
                application.getDescription(),
                application.getImagePath(),
                application.getStatus()
        );
    }
}