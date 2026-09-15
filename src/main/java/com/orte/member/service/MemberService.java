package com.orte.member.service;

import com.orte.member.dto.SignupRequest;
import com.orte.member.entity.Member;
import com.orte.member.exception.EmailAlreadyExistsException;
import com.orte.member.exception.NicknameAlreadyExistsException;
import com.orte.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signup(SignupRequest request) {

        if (memberRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException();
        }

        if (memberRepository.existsByNickname(request.nickname())) {
            throw new NicknameAlreadyExistsException();
        }

        String encodedPassword =
                passwordEncoder.encode(request.password());

        Member member = new Member(
                request.email(),
                encodedPassword,
                request.nickname()
        );

        Member savedMember = memberRepository.save(member);

        return savedMember.getId();
    }
}