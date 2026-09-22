package com.orte.security.jwt;

import com.orte.member.entity.Member;
import com.orte.member.repository.MemberRepository;
import com.orte.security.CustomUserDetails;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("1초 안에 발급한 Refresh Token도 서로 다른 값이어야 한다")
    void refreshTokensIssuedWithinOneSecondAreDifferent() {

        Member member = memberRepository.save(
                new Member(
                        "test@example.com",
                        "encoded-password",
                        "방토"
                )
        );

        CustomUserDetails userDetails =
                new CustomUserDetails(member);

        String firstToken =
                jwtTokenProvider.createRefreshToken(userDetails);

        String secondToken =
                jwtTokenProvider.createRefreshToken(userDetails);

        assertThat(firstToken)
                .isNotEqualTo(secondToken);
    }
}