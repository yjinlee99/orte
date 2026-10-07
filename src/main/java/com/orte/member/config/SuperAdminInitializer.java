package com.orte.member.config;

import com.orte.member.entity.Member;
import com.orte.member.entity.MemberRole;
import com.orte.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SuperAdminInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    private final String email;
    private final String password;
    private final String nickname;

    public SuperAdminInitializer(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            @Value("${SUPER_ADMIN_EMAIL:}") String email,
            @Value("${SUPER_ADMIN_PASSWORD:}") String password,
            @Value("${SUPER_ADMIN_NICKNAME:super-admin}") String nickname
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (email.isBlank() || password.isBlank()) {
            return;
        }

        if (memberRepository.existsByRole(MemberRole.SUPER_ADMIN)) {
            return;
        }

        if (memberRepository.existsByEmail(email)) {
            throw new IllegalStateException(
                    "슈퍼 관리자 이메일과 동일한 회원이 이미 존재합니다."
            );
        }

        if (memberRepository.existsByNickname(nickname)) {
            throw new IllegalStateException(
                    "슈퍼 관리자 닉네임과 동일한 회원이 이미 존재합니다."
            );
        }

        String encodedPassword =
                passwordEncoder.encode(password);

        Member superAdmin =
                Member.createSuperAdmin(
                        email,
                        encodedPassword,
                        nickname
                );

        memberRepository.save(superAdmin);
    }
}