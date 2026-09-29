package com.orte.member.controller;

import com.orte.auth.entity.RefreshToken;
import com.orte.auth.repository.RefreshTokenRepository;
import com.orte.auth.util.RefreshTokenHasher;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthRefreshIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("Access Token으로 재발급을 요청하면 401을 반환한다")
    void accessTokenCannotBeUsedForRefresh() throws Exception {

        // given
        memberRepository.save(
                new Member(
                        "access-test@example.com",
                        passwordEncoder.encode("password123"),
                        "액세스토큰테스트"
                )
        );

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "access-test@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper
                .readTree(loginResponse)
                .get("accessToken")
                .asText();

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(accessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message")
                        .value("유효하지 않은 Refresh Token입니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/auth/refresh"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("로그아웃한 Refresh Token으로 재발급을 요청하면 401을 반환한다")
    void loggedOutRefreshTokenCannotBeReused() throws Exception {

        memberRepository.save(
                new Member(
                        "logout-test@example.com",
                        passwordEncoder.encode("password123"),
                        "로그아웃테스트"
                )
        );

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "logout-test@example.com",
                              "password": "password123"
                            }
                            """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = objectMapper
                .readTree(loginResponse)
                .get("refreshToken")
                .asText();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "refreshToken": "%s"
                            }
                            """.formatted(refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "refreshToken": "%s"
                            }
                            """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.message")
                        .value("유효하지 않은 Refresh Token입니다."));
    }

    @Test
    @DisplayName("로그인 시 Refresh Token은 원문이 아닌 해시값으로 저장된다")
    void refreshTokenIsStoredAsHash() throws Exception {

        memberRepository.save(
                new Member(
                        "hash-test@example.com",
                        passwordEncoder.encode("password123"),
                        "해시테스트"
                )
        );

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "hash-test@example.com",
                              "password": "password123"
                            }
                            """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = objectMapper
                .readTree(loginResponse)
                .get("refreshToken")
                .asText();

        String tokenHash = RefreshTokenHasher.hash(refreshToken);

        RefreshToken savedRefreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow();

        assertThat(savedRefreshToken.getTokenHash())
                .isNotEqualTo(refreshToken);

        assertThat(savedRefreshToken.getTokenHash())
                .isEqualTo(tokenHash);
    }

    @Test
    @DisplayName("Refresh Token은 갱신 시 회전되고 이전 토큰은 재사용할 수 없다")
    void refreshTokenRotationFlow() throws Exception {

        // given - 회원 생성
        Member member = memberRepository.save(
                new Member(
                        "rotation@example.com",
                        passwordEncoder.encode("password123"),
                        "회전테스트"
                )
        );

        // 1. 로그인 → R0 발급
        String loginResponse = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "rotation@example.com",
                                      "password": "password123"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String r0 = objectMapper.readTree(loginResponse)
                .get("refreshToken")
                .asText();

        // 2. R0로 갱신 → R1 발급
        String firstRefreshResponse = mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "refreshToken": "%s"
                                    }
                                    """.formatted(r0))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String r1 = objectMapper.readTree(firstRefreshResponse)
                .get("refreshToken")
                .asText();

        // R0와 R1은 달라야 함
        assertThat(r1).isNotEqualTo(r0);

        // 3. 이미 사용한 R0 재사용 → 401
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "refreshToken": "%s"
                                    }
                                    """.formatted(r0))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"));

        // 4. R1은 정상 사용 가능 → R2 발급
        String secondRefreshResponse = mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "refreshToken": "%s"
                                    }
                                    """.formatted(r1))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String r2 = objectMapper.readTree(secondRefreshResponse)
                .get("refreshToken")
                .asText();

        assertThat(r2).isNotEqualTo(r1);

        // 5. 최신 Refresh Token R2로 로그아웃
        mockMvc.perform(
                        post("/api/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "refreshToken": "%s"
                                    }
                                    """.formatted(r2))
                )
                .andExpect(status().isNoContent());

        // 6. 로그아웃한 R2 재사용 → 401
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "refreshToken": "%s"
                                    }
                                    """.formatted(r2))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"));
    }
}