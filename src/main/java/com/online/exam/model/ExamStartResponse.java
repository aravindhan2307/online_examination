package com.online.exam.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response from POST /api/questions/start.
 * Contains the shuffled, unseen questions for this attempt and a flag
 * indicating whether the question history was reset (all questions exhausted).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamStartResponse {
    private List<QuestionDTO> questions;
    /** True when all questions in the category had been seen and history was auto-reset. */
    private boolean wasReset;
}
