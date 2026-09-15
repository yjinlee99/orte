package com.orte.board.application.controller;

import com.orte.board.application.dto.BoardApplicationCreateRequest;
import com.orte.board.application.entity.BoardApplication;
import com.orte.board.application.entity.BoardApplicationStatus;
import com.orte.board.application.repository.BoardApplicationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    @DisplayName("게시판 개설 신청에 성공한다")
    void createBoardApplication() throws Exception {
        BoardApplicationCreateRequest request =
                new BoardApplicationCreateRequest(
                        "진격의 거인",
                        "진격의 거인 이야기 게시판",
                        "/images/aot.jpg"
                );

        mockMvc.perform(post("/board-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("진격의 거인"))
                .andExpect(jsonPath("$.description").value("진격의 거인 이야기 게시판"))
                .andExpect(jsonPath("$.imagePath").value("/images/aot.jpg"))
                .andExpect(jsonPath("$.applicantId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));

        List<BoardApplication> applications =
                boardApplicationRepository.findAll();

        assertThat(applications).hasSize(1);

        BoardApplication saved = applications.get(0);

        assertThat(saved.getTitle()).isEqualTo("진격의 거인");
        assertThat(saved.getApplicantId()).isEqualTo(1L);
        assertThat(saved.getStatus())
                .isEqualTo(BoardApplicationStatus.PENDING);
    }

    @Test
    @DisplayName("본인의 게시판 개설 신청만 조회한다")
    void getMyBoardApplications() throws Exception {

        boardApplicationRepository.save(
                new BoardApplication(
                        1L,
                        "진격의 거인",
                        "내 신청",
                        "/images/aot.jpg"
                )
        );

        boardApplicationRepository.save(
                new BoardApplication(
                        2L,
                        "원피스",
                        "다른 사용자의 신청",
                        "/images/onepiece.jpg"
                )
        );

        mockMvc.perform(get("/me/board-applications"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].applicantId").value(1))
                .andExpect(jsonPath("$[0].title").value("진격의 거인"));
    }
}