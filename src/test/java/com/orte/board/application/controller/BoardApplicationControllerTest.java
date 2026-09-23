package com.orte.board.application.controller;

import com.orte.board.application.dto.BoardApplicationCreateRequest;
import com.orte.board.application.entity.BoardApplication;
import com.orte.board.application.entity.BoardApplicationStatus;
import com.orte.board.application.repository.BoardApplicationRepository;
import com.orte.member.entity.Member;
import com.orte.member.repository.MemberRepository;
import com.orte.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BoardApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BoardApplicationRepository boardApplicationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private CustomUserDetails userDetails;
    private Long memberId;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(
                new Member(
                        "test@example.com",
                        "encoded-password",
                        "방토"
                )
        );

        memberId = member.getId();
        userDetails = new CustomUserDetails(member);
    }

    @Test
    @DisplayName("게시판 개설 신청에 성공한다")
    void createBoardApplication() throws Exception {

        BoardApplicationCreateRequest request =
                new BoardApplicationCreateRequest(
                        "진격의 거인",
                        "진격의 거인 이야기 게시판"
                );

        mockMvc.perform(post("/api/board-applications")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("진격의 거인"))
                .andExpect(jsonPath("$.description")
                        .value("진격의 거인 이야기 게시판"))
                .andExpect(jsonPath("$.applicantId")
                        .value(memberId))
                .andExpect(jsonPath("$.status")
                        .value("PENDING"));

        List<BoardApplication> applications =
                boardApplicationRepository.findAll();

        assertThat(applications).hasSize(1);

        BoardApplication saved = applications.get(0);

        assertThat(saved.getTitle())
                .isEqualTo("진격의 거인");

        assertThat(saved.getApplicantId())
                .isEqualTo(memberId);

        assertThat(saved.getStatus())
                .isEqualTo(BoardApplicationStatus.PENDING);
    }

    @Test
    @DisplayName("본인의 게시판 개설 신청만 조회한다")
    void getMyBoardApplications() throws Exception {

        boardApplicationRepository.save(
                new BoardApplication(
                        memberId,
                        "진격의 거인",
                        "내 신청"
                )
        );

        boardApplicationRepository.save(
                new BoardApplication(
                        memberId + 100,
                        "원피스",
                        "다른 사용자의 신청"
                )
        );

        mockMvc.perform(get("/api/me/board-applications")
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].applicantId")
                        .value(memberId))
                .andExpect(jsonPath("$[0].title")
                        .value("진격의 거인"));
    }



    @Test
    @DisplayName("게시판 제목이 없으면 개설 신청에 실패한다")
    void createBoardApplicationWithoutTitle() throws Exception {

        String request = """
                {
                  "title": "",
                  "description": "제목 없는 신청"
                }
                """;

        mockMvc.perform(post("/api/board-applications")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message")
                        .value("입력값을 확인해 주세요."))
                .andExpect(jsonPath("$.path")
                        .value("/api/board-applications"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.title")
                        .value("게시판 제목은 필수입니다."));

        assertThat(boardApplicationRepository.count()).isZero();
    }

    @Test
    @DisplayName("여러 입력값이 잘못되면 각 필드의 검증 오류를 반환한다")
    void createBoardApplicationWithMultipleValidationErrors()
            throws Exception {

        String request = """
                {
                  "title": "",
                  "description": ""
                }
                """;

        mockMvc.perform(post("/api/board-applications")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.title")
                        .value("게시판 제목은 필수입니다."))
                .andExpect(jsonPath("$.errors.description")
                        .value("게시판 소개는 필수입니다."));

        assertThat(boardApplicationRepository.count()).isZero();
    }

    @Test
    @DisplayName("잘못된 JSON 형식으로 요청하면 400을 반환한다")
    void createBoardApplicationWithInvalidJson() throws Exception {

        long beforeCount = boardApplicationRepository.count();

        String invalidRequest = """
                {
                  "title": "진격의 거인",
                  "description": "게시판 설명"
                """;

        mockMvc.perform(post("/api/board-applications")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("요청 본문을 확인해 주세요."))
                .andExpect(jsonPath("$.path")
                        .value("/api/board-applications"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertThat(boardApplicationRepository.count())
                .isEqualTo(beforeCount);
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type으로 요청하면 415를 반환한다")
    void createBoardApplicationWithUnsupportedMediaType()
            throws Exception {

        long beforeCount = boardApplicationRepository.count();

        String request = """
                {
                  "title": "진격의 거인",
                  "description": "게시판 설명"
                }
                """;

        mockMvc.perform(post("/api/board-applications")
                        .with(user(userDetails))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(request))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.code")
                        .value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message")
                        .value("지원하지 않는 Content-Type입니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/board-applications"))
                .andExpect(jsonPath("$.timestamp").exists());

        assertThat(boardApplicationRepository.count())
                .isEqualTo(beforeCount);
    }

    @Test
    @DisplayName("게시판 제목이 100자를 초과하면 개설 신청에 실패한다")
    void createBoardApplicationWithTooLongTitle() throws Exception {

        String title = "가".repeat(101);

        String request = """
            {
              "title": "%s",
              "description": "게시판 설명"
            }
            """.formatted(title);

        mockMvc.perform(post("/api/board-applications")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.title")
                        .value("게시판 제목은 100자 이하로 입력해 주세요."));

        assertThat(boardApplicationRepository.count()).isZero();
    }

    @Test
    @DisplayName("게시판 제목이 정확히 100자이면 개설 신청에 성공한다")
    void createBoardApplicationWith100CharacterTitle() throws Exception {

        String title = "가".repeat(100);

        String request = """
            {
              "title": "%s",
              "description": "게시판 설명"
            }
            """.formatted(title);

        mockMvc.perform(post("/api/board-applications")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title")
                        .value(title));

        assertThat(boardApplicationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("게시판 소개가 1000자를 초과하면 개설 신청에 실패한다")
    void createBoardApplicationWithTooLongDescription() throws Exception {

        String description = "가".repeat(1001);

        String request = """
            {
              "title": "진격의 거인",
              "description": "%s"
            }
            """.formatted(description);

        mockMvc.perform(post("/api/board-applications")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.description")
                        .value("게시판 소개는 1000자 이하로 입력해 주세요."));

        assertThat(boardApplicationRepository.count()).isZero();
    }

    @Test
    @DisplayName("게시판 소개가 1000자이면 개설 신청에 성공한다")
    void createBoardApplicationWith1000CharacterDescription() throws Exception {

        String description = "가".repeat(1000);

        String request = """
            {
              "title": "진격의 거인",
              "description": "%s"
            }
            """.formatted(description);

        mockMvc.perform(post("/api/board-applications")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title")
                        .value("진격의 거인"))
                .andExpect(jsonPath("$.description")
                        .value(description));

        assertThat(boardApplicationRepository.count()).isEqualTo(1);
    }


}