package com.orte.board.application.service;

import com.orte.board.application.dto.BoardApplicationCreateRequest;
import com.orte.board.application.dto.BoardApplicationResponse;
import com.orte.board.application.entity.BoardApplication;
import com.orte.board.application.entity.BoardApplicationStatus;
import com.orte.board.application.repository.BoardApplicationRepository;
import com.orte.board.entity.Board;
import com.orte.board.repository.BoardRepository;
import com.orte.exception.BusinessException;
import com.orte.exception.ErrorCode;
import com.orte.member.entity.Member;
import com.orte.member.repository.MemberRepository;
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

    private final BoardApplicationRepository boardApplicationRepository;
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;

    // 게시판 개설 신청 생성
    @Transactional
    public BoardApplicationResponse create(
            Long applicantId,
            BoardApplicationCreateRequest request
    ) {

        BoardApplication application = new BoardApplication(
                applicantId,
                request.title(),
                request.description()
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

    // 내 게시판 신청 목록
    public List<BoardApplicationResponse> getMyApplications(
            Long applicantId
    ) {

        return boardApplicationRepository
                .findAllByApplicantIdOrderByCreatedAtDesc(applicantId)
                .stream()
                .map(BoardApplicationResponse::from)
                .toList();
    }

    @Transactional
    public void approve(Long applicationId) {

        BoardApplication application =
                boardApplicationRepository.findByIdForUpdate(applicationId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.BOARD_APPLICATION_NOT_FOUND
                                )
                        );

        if (application.getStatus() != BoardApplicationStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.BOARD_APPLICATION_ALREADY_PROCESSED
            );
        }

        Member owner =
                memberRepository.getReferenceById(
                        application.getApplicantId()
                );

        Board board = new Board(
                application.getTitle(),
                application.getDescription(),
                owner
        );

        boardRepository.save(board);

        application.approve();
    }
}