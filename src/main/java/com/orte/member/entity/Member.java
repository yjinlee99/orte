package com.orte.member.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Member(
            String email,
            String password,
            String nickname
    ) {
        this(
                email,
                password,
                nickname,
                MemberRole.USER
        );
    }

    private Member(
            String email,
            String password,
            String nickname,
            MemberRole role
    ) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    public static Member createSuperAdmin(
            String email,
            String password,
            String nickname
    ) {
        return new Member(
                email,
                password,
                nickname,
                MemberRole.SUPER_ADMIN
        );
    }

    public void promoteToAdmin() {
        this.role = MemberRole.ADMIN;
    }

}