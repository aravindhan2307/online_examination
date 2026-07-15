package com.online.exam.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamGradingResponse {
    private String userName;
    private int score;
    private int totalQuestions;
    private double percentage;
    private List<QuestionResult> questionResults;
}
