package com.online.exam.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResult {
    private Long questionId;
    private String questionText;
    private List<String> options;
    private int selectedOptionIndex;
    private int correctOptionIndex;
    private boolean isCorrect;
}
