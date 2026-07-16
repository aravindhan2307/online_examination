package com.online.exam.repository;

import com.online.exam.entity.StudentQuestionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentQuestionHistoryRepository extends JpaRepository<StudentQuestionHistory, Long> {

    Optional<StudentQuestionHistory> findByCandidateNameIgnoreCaseAndCategoryIgnoreCase(
            String candidateName, String category);
}
