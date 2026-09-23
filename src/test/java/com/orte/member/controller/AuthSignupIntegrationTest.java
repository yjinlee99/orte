package com.orte.member.controller;

import com.orte.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthSignupIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("비밀번호가 8자 미만이면 회원가입에 실패한다")
    void signupWithShortPassword() throws Exception {

        String request = """
                {
                  "email": "test@example.com",
                  "password": "1234567",
                  "nickname": "방토"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.password")
                        .value("비밀번호는 8자 이상 64자 이하로 입력해 주세요."));

        assertThat(memberRepository.count()).isZero();
    }

    @Test
    @DisplayName("비밀번호가 8자이면 회원가입에 성공한다")
    void signupWithEightCharacterPassword() throws Exception {

        String request = """
            {
              "email": "test@example.com",
              "password": "12345678",
              "nickname": "방토"
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").exists());

        assertThat(
                memberRepository.findByEmail("test@example.com")
        ).isPresent();
    }

    @Test
    @DisplayName("비밀번호가 정확히 64자이면 회원가입에 성공한다")
    void signupWith64CharacterPassword() throws Exception {

        String password = "a".repeat(64);

        String request = """
            {
              "email": "test@example.com",
              "password": "%s",
              "nickname": "방토"
            }
            """.formatted(password);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").exists());

        assertThat(
                memberRepository.findByEmail("test@example.com")
        ).isPresent();
    }

    @Test
    @DisplayName("비밀번호가 64자를 초과하면 회원가입에 실패한다")
    void signupWithTooLongPassword() throws Exception {

        String password = "a".repeat(65);

        String request = """
            {
              "email": "test@example.com",
              "password": "%s",
              "nickname": "방토"
            }
            """.formatted(password);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.password")
                        .value("비밀번호는 8자 이상 64자 이하로 입력해 주세요."));

        assertThat(memberRepository.count()).isZero();
    }

    @Test
    @DisplayName("닉네임이 2자 미만이면 회원가입에 실패한다")
    void signupWithShortNickname() throws Exception {

        String request = """
            {
              "email": "test@example.com",
              "password": "password123",
              "nickname": "방"
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.nickname")
                        .value("닉네임은 2자 이상 20자 이하로 입력해 주세요."));

        assertThat(memberRepository.count()).isZero();
    }

    @Test
    @DisplayName("닉네임이 정확히 2자이면 회원가입에 성공한다")
    void signupWithTwoCharacterNickname() throws Exception {

        String request = """
            {
              "email": "test@example.com",
              "password": "password123",
              "nickname": "방토"
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").exists());

        assertThat(
                memberRepository.findByEmail("test@example.com")
        ).isPresent();
    }

    @Test
    @DisplayName("닉네임이 정확히 20자이면 회원가입에 성공한다")
    void signupWith20CharacterNickname() throws Exception {

        String nickname = "가".repeat(20);

        String request = """
            {
              "email": "test@example.com",
              "password": "password123",
              "nickname": "%s"
            }
            """.formatted(nickname);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").exists());

        assertThat(
                memberRepository.findByEmail("test@example.com")
        ).isPresent();
    }

    @Test
    @DisplayName("닉네임이 20자를 초과하면 회원가입에 실패한다")
    void signupWithTooLongNickname() throws Exception {

        String nickname = "가".repeat(21);

        String request = """
            {
              "email": "test@example.com",
              "password": "password123",
              "nickname": "%s"
            }
            """.formatted(nickname);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.nickname")
                        .value("닉네임은 2자 이상 20자 이하로 입력해 주세요."));

        assertThat(memberRepository.count()).isZero();
    }
}
