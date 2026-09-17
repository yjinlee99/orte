package com.orte.auth.service;

import com.orte.auth.entity.RefreshToken;
import com.orte.auth.exception.InvalidRefreshTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import com.orte.member.dto.LoginRequest;
import com.orte.member.dto.LoginResponse;
import com.orte.member.entity.Member;
import com.orte.member.repository.MemberRepository;
import com.orte.security.CustomUserDetails;
import com.orte.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final MemberRepository memberRepository;

    public LoginResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        String accessToken =
                jwtTokenProvider.createAccessToken(userDetails);

        String refreshToken =
                jwtTokenProvider.createRefreshToken(userDetails);

        Member member =
                memberRepository.getReferenceById(userDetails.getMemberId());

        refreshTokenService.save(member, refreshToken);

        return new LoginResponse(
                accessToken,
                refreshToken
        );
    }

    public LoginResponse refresh(String refreshToken) {

        try {
            Claims claims = jwtTokenProvider.getClaims(refreshToken);

            if (!"REFRESH".equals(claims.get("tokenType", String.class))) {
                throw new InvalidRefreshTokenException();
            }
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidRefreshTokenException();
        }

        RefreshToken savedToken =
                refreshTokenService.findByToken(refreshToken);

        Member member = savedToken.getMember();

        CustomUserDetails userDetails =
                new CustomUserDetails(member);

        // 기존 Refresh Token 폐기
        refreshTokenService.deleteByToken(refreshToken);

        // 새 토큰 발급
        String newAccessToken =
                jwtTokenProvider.createAccessToken(userDetails);

        String newRefreshToken =
                jwtTokenProvider.createRefreshToken(userDetails);

        // 새 Refresh Token 저장
        refreshTokenService.save(member, newRefreshToken);

        return new LoginResponse(
                newAccessToken,
                newRefreshToken
        );
    }

    public void logout(String refreshToken) {
        refreshTokenService.deleteByToken(refreshToken);
    }

}
