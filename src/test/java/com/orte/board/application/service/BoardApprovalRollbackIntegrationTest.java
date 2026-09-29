package com.orte.board.application.service;

import com.orte.board.application.entity.BoardApplication;
import com.orte.board.application.entity.BoardApplicationStatus;
import com.orte.board.application.repository.BoardApplicationRepository;
import com.orte.board.entity.Board;
import com.orte.board.repository.BoardRepository;
import com.orte.member.entity.Member;
import com.orte.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
public class BoardApprovalRollbackIntegrationTest {

    @Autowired
    BoardApplicationService boardApplicationService;

    @Autowired
    BoardApplicationRepository boardApplicationRepository;

    @Autowired
    MemberRepository memberRepository;

    @MockitoSpyBean
    BoardRepository boardRepository;

    @PersistenceContext
    EntityManager entityManager;

    @AfterEach
    void tearDown() {
        boardRepository.deleteAll();
        boardApplicationRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("게시판 승인 중 실패하면 게시판 생성과 신청 상태 변경이 모두 롤백된다")
    void approvalFailureRollsBackAllChanges() {

        // given
        Member applicant = memberRepository.save(
                new Member(
                        "rollback@example.com",
                        "encoded-password",
                        "롤백테스트"
                )
        );

        BoardApplication application =
                boardApplicationRepository.save(
                        new BoardApplication(
                                applicant.getId(),
                                "블루 록",
                                "롤백 테스트용 게시판"
                        )
                );

        doAnswer(invocation -> {

            Board board = invocation.getArgument(0);

            entityManager.persist(board);
            entityManager.flush();

            throw new IllegalStateException("승인 중 강제 실패");

        }).when(boardRepository).save(any(Board.class));

        // when
        assertThatThrownBy(() ->
                boardApplicationService.approve(application.getId())
        ).isInstanceOf(IllegalStateException.class);

        // then
        assertThat(boardRepository.count())
                .isZero();

        BoardApplication rolledBackApplication =
                boardApplicationRepository
                        .findById(application.getId())
                        .orElseThrow();

        assertThat(rolledBackApplication.getStatus())
                .isEqualTo(BoardApplicationStatus.PENDING);
    }
}
