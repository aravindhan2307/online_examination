package com.online.exam.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * View model for the admin Arrear Students List page.
 * Combines the latest arrear ExamResult with the corresponding FeePayment status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArrearStudentDTO {
    private String candidateName;
    private String category;
    private int score;
    private int totalQuestions;
    private double percentage;
    private LocalDateTime submissionTime;
    private boolean feePaid;
    private Long feeId;
    private double feeAmount;
}
