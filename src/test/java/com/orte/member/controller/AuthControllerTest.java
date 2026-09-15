package com.orte.member.controller;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입에 성공한다")
    void signup() throws Exception {

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

        Member member = memberRepository.findByEmail("test@example.com")
                .orElseThrow();

        assertThat(member.getNickname()).isEqualTo("방토");
        assertThat(member.getRole().name()).isEqualTo("USER");

        // 평문으로 저장되지 않았는지
        assertThat(member.getPassword()).isNotEqualTo("password123");

        // 실제 BCrypt 검증까지
        assertThat(passwordEncoder.matches(
                "password123",
                member.getPassword()
        )).isTrue();
    }

    @Test
    @DisplayName("이미 사용 중인 이메일로 회원가입하면 409를 반환한다")
    void signupWithDuplicatedEmail() throws Exception {

        memberRepository.save(
                new Member(
                        "test@example.com",
                        passwordEncoder.encode("password123"),
                        "첫회원"
                )
        );

        String request = """
            {
              "email": "test@example.com",
              "password": "password456",
              "nickname": "다른회원"
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message")
                        .value("이미 사용 중인 이메일입니다."));
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임으로 회원가입하면 409를 반환한다")
    void signupWithDuplicatedNickname() throws Exception {

        memberRepository.save(
                new Member(
                        "first@example.com",
                        passwordEncoder.encode("password123"),
                        "방토"
                )
        );

        String request = """
            {
              "email": "second@example.com",
              "password": "password456",
              "nickname": "방토"
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("NICKNAME_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message")
                        .value("이미 사용 중인 닉네임입니다."));
    }

    @Test
    @DisplayName("이메일 형식이 잘못되면 회원가입에 실패한다")
    void signupWithInvalidEmail() throws Exception {

        String request = """
            {
              "email": "not-email",
              "password": "password123",
              "nickname": "방토"
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.email")
                        .value("올바른 이메일 형식이 아닙니다."));

        assertThat(memberRepository.count()).isZero();
    }
}