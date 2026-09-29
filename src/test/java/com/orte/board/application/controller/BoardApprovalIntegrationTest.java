package com.orte.board.application.controller;

import com.orte.auth.repository.RefreshTokenRepository;
import com.orte.board.application.entity.BoardApplication;
import com.orte.board.application.entity.BoardApplicationStatus;
import com.orte.board.application.repository.BoardApplicationRepository;
import com.orte.board.entity.Board;
import com.orte.board.repository.BoardRepository;
import com.orte.member.entity.Member;
import com.orte.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BoardApprovalIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    BoardApplicationRepository boardApplicationRepository;

    @Autowired
    BoardRepository boardRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        boardRepository.deleteAll();
        boardApplicationRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("일반 회원이 게시판 개설 승인을 요청하면 403이고 데이터는 변경되지 않는다")
    void userCannotApproveBoardApplication() throws Exception {

        // given
        Member member = memberRepository.save(
                new Member(
                        "user@example.com",
                        passwordEncoder.encode("password123"),
                        "일반회원"
                )
        );

        BoardApplication application =
                boardApplicationRepository.save(
                        new BoardApplication(
                                member.getId(),
                                "진격의 거인",
                                "진격의 거인 게시판"
                        )
                );

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "email": "user@example.com",
                          "password": "password123"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken =
                objectMapper.readTree(loginResponse)
                        .get("accessToken")
                        .asText();

        // 이 토큰이 진짜 유효한 USER 토큰인지 먼저 확인
        mockMvc.perform(get("/api/me/board-applications")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        ))
                .andExpect(status().isOk());

        long boardCountBefore = boardRepository.count();

        // when & then
        mockMvc.perform(post(
                        "/api/board-applications/{id}/approve",
                        application.getId()
                )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        ))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message")
                        .value("접근 권한이 없습니다."));

        assertThat(boardRepository.count())
                .isEqualTo(boardCountBefore);

        BoardApplication unchanged =
                boardApplicationRepository
                        .findById(application.getId())
                        .orElseThrow();

        assertThat(unchanged.getStatus())
                .isEqualTo(BoardApplicationStatus.PENDING);
    }

    @Test
    @DisplayName("운영자가 대기 중인 게시판 개설 신청을 승인하면 게시판이 생성되고 신청자가 방장이 된다")
    void adminCanApproveBoardApplication() throws Exception {

        // given - 신청자
        Member applicant = memberRepository.save(
                new Member(
                        "applicant@example.com",
                        passwordEncoder.encode("password123"),
                        "신청자"
                )
        );

        BoardApplication application =
                boardApplicationRepository.save(
                        new BoardApplication(
                                applicant.getId(),
                                "진격의 거인",
                                "진격의 거인 게시판"
                        )
                );

        // given - 운영자
        Member admin = new Member(
                "admin@example.com",
                passwordEncoder.encode("password123"),
                "관리자"
        );

        admin.promoteToAdmin();
        memberRepository.save(admin);

        String adminAccessToken =
                loginAndGetAccessToken(
                        "admin@example.com",
                        "password123"
                );

        // when
        mockMvc.perform(
                        post(
                                "/api/board-applications/{id}/approve",
                                application.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                )
                .andExpect(status().isNoContent());

        // then - Board 생성 확인
        assertThat(boardRepository.count()).isEqualTo(1);

        Board board = boardRepository.findAll()
                .get(0);

        assertThat(board.getTitle())
                .isEqualTo("진격의 거인");

        assertThat(board.getOwner().getId())
                .isEqualTo(applicant.getId());

        // then - 신청 상태 변경 확인
        BoardApplication approvedApplication =
                boardApplicationRepository
                        .findById(application.getId())
                        .orElseThrow();

        assertThat(approvedApplication.getStatus())
                .isEqualTo(BoardApplicationStatus.APPROVED);
    }

    private String loginAndGetAccessToken(
            String email,
            String password
    ) throws Exception {

        String response = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "%s",
                                      "password": "%s"
                                    }
                                    """.formatted(email, password))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper
                .readTree(response)
                .get("accessToken")
                .asText();
    }

    @Test
    @DisplayName("이미 승인된 신청을 다시 승인하면 409를 반환하고 게시판은 추가 생성되지 않는다")
    void alreadyApprovedApplicationCannotBeApprovedAgain() throws Exception {

        // given - 신청자
        Member applicant = memberRepository.save(
                new Member(
                        "applicant2@example.com",
                        passwordEncoder.encode("password123"),
                        "신청자2"
                )
        );

        BoardApplication application =
                boardApplicationRepository.save(
                        new BoardApplication(
                                applicant.getId(),
                                "원피스",
                                "원피스 게시판"
                        )
                );

        // given - 운영자
        Member admin = new Member(
                "admin2@example.com",
                passwordEncoder.encode("password123"),
                "관리자2"
        );

        admin.promoteToAdmin();
        memberRepository.save(admin);

        String adminAccessToken =
                loginAndGetAccessToken(
                        "admin2@example.com",
                        "password123"
                );

        // 첫 번째 승인
        mockMvc.perform(
                        post(
                                "/api/board-applications/{id}/approve",
                                application.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                )
                .andExpect(status().isNoContent());

        assertThat(boardRepository.count())
                .isEqualTo(1);

        // when - 같은 신청을 다시 승인
        mockMvc.perform(
                        post(
                                "/api/board-applications/{id}/approve",
                                application.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + adminAccessToken
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("BOARD_APPLICATION_ALREADY_PROCESSED"));

        // then - 게시판이 추가 생성되지 않음
        assertThat(boardRepository.count())
                .isEqualTo(1);

        BoardApplication approvedApplication =
                boardApplicationRepository
                        .findById(application.getId())
                        .orElseThrow();

        assertThat(approvedApplication.getStatus())
                .isEqualTo(BoardApplicationStatus.APPROVED);
    }
}
