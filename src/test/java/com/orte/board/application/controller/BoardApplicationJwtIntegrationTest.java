package com.orte.board.application.controller;

import com.orte.member.entity.Member;
import com.orte.member.repository.MemberRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BoardApplicationJwtIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("실제 JWT 인증으로 회원별 게시판 신청을 구분한다")
    void getMyBoardApplicationsWithJwt() throws Exception {

        // given
        Member memberA = memberRepository.save(
                new Member(
                        "memberA@example.com",
                        passwordEncoder.encode("password123"),
                        "회원A"
                )
        );

        Member memberB = memberRepository.save(
                new Member(
                        "memberB@example.com",
                        passwordEncoder.encode("password123"),
                        "회원B"
                )
        );

        String accessTokenA = loginAndGetAccessToken(
                "memberA@example.com",
                "password123"
        );

        String accessTokenB = loginAndGetAccessToken(
                "memberB@example.com",
                "password123"
        );

        // when - A 신청
        mockMvc.perform(
                        post("/api/board-applications")
                                .header(
                                        "Authorization",
                                        "Bearer " + accessTokenA
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "진격의 거인",
                                          "description": "A의 신청"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicantId")
                        .value(memberA.getId()));

        // when - B 신청
        mockMvc.perform(
                        post("/api/board-applications")
                                .header(
                                        "Authorization",
                                        "Bearer " + accessTokenB
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "원피스",
                                          "description": "B의 신청"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicantId")
                        .value(memberB.getId()));

        // then - A는 A의 신청만 조회
        mockMvc.perform(
                        get("/api/me/board-applications")
                                .header(
                                        "Authorization",
                                        "Bearer " + accessTokenA
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].applicantId")
                        .value(memberA.getId()))
                .andExpect(jsonPath("$[0].title")
                        .value("진격의 거인"));

        // then - B는 B의 신청만 조회
        mockMvc.perform(
                        get("/api/me/board-applications")
                                .header(
                                        "Authorization",
                                        "Bearer " + accessTokenB
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].applicantId")
                        .value(memberB.getId()))
                .andExpect(jsonPath("$[0].title")
                        .value("원피스"));
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
}
