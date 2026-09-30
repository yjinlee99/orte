package com.orte.board.application.repository;

import com.orte.board.application.entity.BoardApplication;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface BoardApplicationRepository
        extends JpaRepository<BoardApplication, Long> {

    List<BoardApplication> findAllByApplicantIdOrderByCreatedAtDesc(Long applicantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ba from BoardApplication ba where ba.id = :id")
    Optional<BoardApplication> findByIdForUpdate(@Param("id") Long id);
}
