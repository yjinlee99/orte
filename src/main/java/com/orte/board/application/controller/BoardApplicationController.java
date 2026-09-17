package com.orte.board.application.controller;

import com.orte.board.application.dto.BoardApplicationCreateRequest;
import com.orte.board.application.dto.BoardApplicationResponse;
import com.orte.board.application.service.BoardApplicationService;
import com.orte.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BoardApplicationController {

    private final BoardApplicationService boardApplicationService;

    @PostMapping("/api/board-applications")
    public ResponseEntity<BoardApplicationResponse> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BoardApplicationCreateRequest request
    ) {
        BoardApplicationResponse response =
                boardApplicationService.create(
                        userDetails.getMemberId(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/api/me/board-applications")
    public ResponseEntity<List<BoardApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<BoardApplicationResponse> responses =
                boardApplicationService.getMyApplications(
                        userDetails.getMemberId()
                );

        return ResponseEntity.ok(responses);
    }
}
