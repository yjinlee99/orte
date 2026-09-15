package com.orte.board.application.controller;

import com.orte.board.application.dto.BoardApplicationCreateRequest;
import com.orte.board.application.dto.BoardApplicationResponse;
import com.orte.board.application.service.BoardApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BoardApplicationController {

    private final BoardApplicationService boardApplicationService;

    @PostMapping("/board-applications")
    public ResponseEntity<BoardApplicationResponse> create(
            @Valid @RequestBody BoardApplicationCreateRequest request
    ) {
        BoardApplicationResponse response =
                boardApplicationService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/me/board-applications")
    public ResponseEntity<List<BoardApplicationResponse>> getMyApplications() {

        List<BoardApplicationResponse> responses =
                boardApplicationService.getMyApplications();

        return ResponseEntity.ok(responses);
    }
}
