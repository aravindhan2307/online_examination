package com.online.exam.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body sent by the candidate login form to start an exam session.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamStartRequest {
    private String candidateName;
    private String category;
}
