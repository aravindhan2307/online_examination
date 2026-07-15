package com.online.exam.service;

import com.online.exam.entity.ExamResult;
import com.online.exam.entity.Question;
import com.online.exam.model.AnswerSubmission;
import com.online.exam.model.ExamGradingResponse;
import com.online.exam.model.ExamSubmission;
import com.online.exam.model.QuestionResult;
import com.online.exam.repository.ExamResultRepository;
import com.online.exam.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExamResultService {

    private final ExamResultRepository examResultRepository;
    private final QuestionRepository questionRepository;

    @Autowired
    public ExamResultService(ExamResultRepository examResultRepository, QuestionRepository questionRepository) {
        this.examResultRepository = examResultRepository;
        this.questionRepository = questionRepository;
    }

    public List<ExamResult> getAllResults() {
        // Retrieve and sort by newest submission time using streams
        return examResultRepository.findAll().stream()
                .sorted((r1, r2) -> r2.getSubmissionTime().compareTo(r1.getSubmissionTime()))
                .collect(Collectors.toList());
    }

    public boolean deleteResult(Long id) {
        return examResultRepository.findById(id).map(res -> {
            examResultRepository.delete(res);
            return true;
        }).orElse(false);
    }

    public ExamGradingResponse gradeAndSaveSubmission(ExamSubmission submission) {
        String category = submission.getCategory();
        
        // 1. Fetch questions of category from DB
        List<Question> questions = questionRepository.findByCategoryIgnoreCase(category);

        // 2. Map submissions using Stream to a lookup map (questionId -> selectedOptionIndex)
        Map<Long, Integer> answerMap = submission.getAnswers().stream()
                .collect(Collectors.toMap(
                        AnswerSubmission::getQuestionId,
                        AnswerSubmission::getSelectedOptionIndex,
                        (existing, replacement) -> replacement // avoid duplicate collisions
                ));

        // 3. Process grades using stream mapping
        List<QuestionResult> questionResults = questions.stream().map(q -> {
            int selectedIndex = answerMap.getOrDefault(q.getId(), -1);
            boolean isCorrect = (selectedIndex == q.getCorrectOptionIndex());
            List<String> options = List.of(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD());
            
            return new QuestionResult(
                    q.getId(),
                    q.getQuestionText(),
                    options,
                    selectedIndex,
                    q.getCorrectOptionIndex(),
                    isCorrect
            );
        }).collect(Collectors.toList());

        // 4. Calculate final score using streams filtering
        int score = (int) questionResults.stream()
                .filter(QuestionResult::isCorrect)
                .count();

        int totalQuestions = questions.size();
        double percentage = totalQuestions > 0 ? ((double) score / totalQuestions) * 100 : 0.0;
        percentage = Math.round(percentage * 10.0) / 10.0; // round to 1 decimal

        // 5. Store summary result in the database
        ExamResult examResult = new ExamResult();
        examResult.setCandidateName(submission.getUserName());
        examResult.setCategory(category);
        examResult.setScore(score);
        examResult.setTotalQuestions(totalQuestions);
        examResult.setPercentage(percentage);
        examResult.setSubmissionTime(LocalDateTime.now());
        examResultRepository.save(examResult);

        // 6. Return response DTO containing review details
        return new ExamGradingResponse(
                submission.getUserName(),
                score,
                totalQuestions,
                percentage,
                questionResults
        );
    }
}
