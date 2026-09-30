package com.orte.board.application.service;

import com.orte.board.application.entity.BoardApplication;
import com.orte.board.application.repository.BoardApplicationRepository;
import com.orte.board.repository.BoardRepository;
import com.orte.exception.BusinessException;
import com.orte.member.entity.Member;
import com.orte.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BoardApprovalConcurrencyIntegrationTest {

    @Autowired
    BoardApplicationService boardApplicationService;

    @Autowired
    BoardApplicationRepository boardApplicationRepository;

    @Autowired
    BoardRepository boardRepository;

    @Autowired
    MemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        boardRepository.deleteAll();
        boardApplicationRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 게시판 신청을 동시에 승인해도 게시판은 하나만 생성된다")
    void concurrentApprovalCreatesOnlyOneBoard() throws Exception {

        //given

        Member applicant = memberRepository.save(
                new Member(
                        "concurrency@example.com",
                        "encoded-password",
                        "동시성테스트"
                )
        );

        BoardApplication application =
                boardApplicationRepository.save(
                        new BoardApplication(
                                applicant.getId(),
                                "주술회전",
                                "동시 승인 테스트용 게시판"
                        )
                );

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<?> first = executorService.submit(() -> {
            readyLatch.countDown();
            startLatch.await();

            boardApplicationService.approve(application.getId());
            return null;
        });

        Future<?> second = executorService.submit(() -> {
            readyLatch.countDown();
            startLatch.await();

            boardApplicationService.approve(application.getId());
            return null;
        });

        readyLatch.await();
        startLatch.countDown();

        int successCount = 0;
        int failureCount = 0;

        for (Future<?> future : List.of(first, second)) {
            try {
                future.get();
                successCount++;
            } catch (ExecutionException e) {
                failureCount++;

                assertThat(e.getCause())
                        .isInstanceOf(BusinessException.class);
            }
        }

        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);
        assertThat(boardRepository.count()).isEqualTo(1);

        executorService.shutdown();
    }


}
