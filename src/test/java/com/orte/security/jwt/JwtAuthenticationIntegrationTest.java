package com.orte.security.jwt;


import com.orte.member.entity.Member;
import com.orte.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
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
class JwtAuthenticationIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ObjectMapper objectMapper;


    @Test
    @DisplayName("토큰 없이 보호 API에 접근하면 401을 반환한다")
    void protectedApiWithoutTokenReturnsUnauthorized() throws Exception {

        mockMvc.perform(get("/api/me/board-applications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.path").value("/api/me/board-applications"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("변조된 Access Token으로 보호 API에 접근하면 401을 반환한다")
    void tamperedAccessTokenReturnsUnauthorized() throws Exception {

        memberRepository.save(
                new Member(
                        "jwt-test@example.com",
                        passwordEncoder.encode("password123"),
                        "JWT테스트"
                )
        );

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "jwt-test@example.com",
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

        String[] tokenParts = accessToken.split("\\.");

        String signature = tokenParts[2];

        char firstChar = signature.charAt(0);
        char replacement = firstChar == 'A' ? 'B' : 'A';

        String tamperedSignature =
                replacement + signature.substring(1);

        String tamperedToken =
                tokenParts[0]
                        + "."
                        + tokenParts[1]
                        + "."
                        + tamperedSignature;

        mockMvc.perform(get("/api/me/board-applications")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + tamperedToken
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message")
                        .value("인증이 필요합니다."))
                .andExpect(jsonPath("$.path")
                        .value("/api/me/board-applications"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Refresh Token으로 보호 API에 접근하면 401을 반환한다")
    void refreshTokenCannotAccessProtectedApi() throws Exception {

        memberRepository.save(
                new Member(
                        "refresh-test@example.com",
                        passwordEncoder.encode("password123"),
                        "리프레시테스트"
                )
        );

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "refresh-test@example.com",
                              "password": "password123"
                            }
                            """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken =
                objectMapper.readTree(loginResponse)
                        .get("refreshToken")
                        .asText();

        mockMvc.perform(get("/api/me/board-applications")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + refreshToken
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message")
                        .value("인증이 필요합니다."));
    }


}
