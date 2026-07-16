package com.online.exam.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Persists which question IDs a candidate has already seen per category,
 * so that repeat questions are never served across different exam attempts.
 * seenQuestionIds is stored as a comma-separated string of Long IDs.
 */
@Entity
@Table(name = "student_question_history",
       uniqueConstraints = @UniqueConstraint(columnNames = {"candidate_name", "category"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentQuestionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_name", nullable = false)
    private String candidateName;

    @Column(nullable = false)
    private String category;

    /** Comma-separated list of question IDs already seen by this candidate in this category. */
    @Column(name = "seen_question_ids", columnDefinition = "TEXT")
    private String seenQuestionIds;
}
