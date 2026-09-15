package com.orte.board.application.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicantId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    private String imagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardApplicationStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public BoardApplication(
            Long applicantId,
            String title,
            String description,
            String imagePath
    ) {
        this.applicantId = applicantId;
        this.title = title;
        this.description = description;
        this.imagePath = imagePath;
        this.status = BoardApplicationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }
}