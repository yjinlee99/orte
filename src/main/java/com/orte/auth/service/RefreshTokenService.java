package com.orte.auth.service;

import com.orte.auth.entity.RefreshToken;
import com.orte.auth.exception.InvalidRefreshTokenException;
import com.orte.auth.repository.RefreshTokenRepository;
import com.orte.auth.util.RefreshTokenHasher;
import com.orte.member.entity.Member;
import com.orte.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public void save(Member member, String refreshToken) {
        String tokenHash = RefreshTokenHasher.hash(refreshToken);

        Instant expiresAt = jwtTokenProvider
                .getClaims(refreshToken)
                .getExpiration()
                .toInstant();

        RefreshToken token = new RefreshToken(
                member,
                tokenHash,
                expiresAt
        );

        refreshTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public RefreshToken findByToken(String refreshToken) {
        String tokenHash = RefreshTokenHasher.hash(refreshToken);

        RefreshToken savedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (savedToken.isExpired()) {
            throw new InvalidRefreshTokenException();
        }

        return savedToken;
    }

    public void deleteByToken(String refreshToken) {
        String tokenHash = RefreshTokenHasher.hash(refreshToken);

        refreshTokenRepository.deleteByTokenHash(tokenHash);
    }

}