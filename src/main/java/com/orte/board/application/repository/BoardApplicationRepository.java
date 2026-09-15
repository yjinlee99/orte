package com.orte.board.application.repository;

import com.orte.board.application.entity.BoardApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardApplicationRepository
        extends JpaRepository<BoardApplication, Long> {

    List<BoardApplication> findAllByApplicantIdOrderByCreatedAtDesc(Long applicantId);
}
