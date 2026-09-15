package com.orte.board.application.service;

import com.orte.board.application.dto.BoardApplicationCreateRequest;
import com.orte.board.application.dto.BoardApplicationResponse;
import com.orte.board.application.entity.BoardApplication;
import com.orte.board.application.repository.BoardApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardApplicationService {

    private static final Long TEST_USER_ID = 1L;

    private final BoardApplicationRepository boardApplicationRepository;

    // 게시판 개설 신청 생성
    @Transactional
    public BoardApplicationResponse create(BoardApplicationCreateRequest request) {

        BoardApplication application = new BoardApplication(
                TEST_USER_ID,
                request.title(),
                request.description(),
                request.imagePath()
        );

        BoardApplication savedApplication =
                boardApplicationRepository.save(application);

        log.info(
                "Board application created. applicationId={}, applicantId={}, status={}",
                savedApplication.getId(),
                savedApplication.getApplicantId(),
                savedApplication.getStatus()
        );

        return BoardApplicationResponse.from(savedApplication);
    }

    // 내 게시판 신청 목록 보여주기
    public List<BoardApplicationResponse> getMyApplications() {

        return boardApplicationRepository
                .findAllByApplicantIdOrderByCreatedAtDesc(TEST_USER_ID)
                .stream()
                .map(BoardApplicationResponse::from)
                .toList();
    }
}