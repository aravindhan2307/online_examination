package com.online.exam.service;

import com.online.exam.entity.ExamResult;
import com.online.exam.entity.Question;
import com.online.exam.entity.StudentQuestionHistory;
import com.online.exam.model.AnswerSubmission;
import com.online.exam.model.ExamGradingResponse;
import com.online.exam.model.ExamSubmission;
import com.online.exam.model.QuestionResult;
import com.online.exam.repository.ExamResultRepository;
import com.online.exam.repository.QuestionRepository;
import com.online.exam.repository.StudentQuestionHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExamResultService {

    private final ExamResultRepository examResultRepository;
    private final QuestionRepository questionRepository;
    private final StudentQuestionHistoryRepository historyRepository;
    private final SessionService sessionService;
    private final FeePaymentService feePaymentService;

    @Autowired
    public ExamResultService(ExamResultRepository examResultRepository,
                             QuestionRepository questionRepository,
                             StudentQuestionHistoryRepository historyRepository,
                             SessionService sessionService,
                             FeePaymentService feePaymentService) {
        this.examResultRepository = examResultRepository;
        this.questionRepository = questionRepository;
        this.historyRepository = historyRepository;
        this.sessionService = sessionService;
        this.feePaymentService = feePaymentService;
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
        String candidateName = submission.getUserName();

        // 1. Fetch questions of category from DB
        List<Question> questions = questionRepository.findByCategoryIgnoreCase(category);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 2. Process grades using the order submitted by the candidate
        List<QuestionResult> questionResults = submission.getAnswers().stream().map(ans -> {
            Question q = questionMap.get(ans.getQuestionId());
            if (q == null) {
                return null;
            }
            int selectedIndex = ans.getSelectedOptionIndex();
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
        })
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toList());

        // 3. Calculate final score using streams filtering
        int score = (int) questionResults.stream()
                .filter(QuestionResult::isCorrect)
                .count();

        int totalQuestions = questionResults.size();
        double percentage = totalQuestions > 0 ? ((double) score / totalQuestions) * 100 : 0.0;
        percentage = Math.round(percentage * 10.0) / 10.0; // round to 1 decimal

        // 4. Store summary result in the database
        boolean isArrear = percentage < 50.0;
        ExamResult examResult = new ExamResult();
        examResult.setCandidateName(candidateName);
        examResult.setCategory(category);
        examResult.setScore(score);
        examResult.setTotalQuestions(totalQuestions);
        examResult.setPercentage(percentage);
        examResult.setArrear(isArrear);
        examResult.setSubmissionTime(LocalDateTime.now());
        examResultRepository.save(examResult);

        // 4b. Auto-create re-exam fee if student is in arrear
        if (isArrear) {
            feePaymentService.createFeeForArrear(candidateName, category);
        }

        // 5. Persist the seen question IDs into StudentQuestionHistory
        persistQuestionHistory(candidateName, category, submission.getAnswers());

        // 6. Release the active session so the candidate can start a new one
        sessionService.endSession(candidateName, category);

        // 7. Return response DTO containing review details
        return new ExamGradingResponse(
                candidateName,
                score,
                totalQuestions,
                percentage,
                questionResults
        );
    }

    /**
     * Appends the newly answered question IDs to the candidate's question history
     * for the given category, creating the history record if it does not yet exist.
     */
    private void persistQuestionHistory(String candidateName, String category,
                                        List<AnswerSubmission> answers) {
        Optional<StudentQuestionHistory> existing =
                historyRepository.findByCandidateNameIgnoreCaseAndCategoryIgnoreCase(
                        candidateName, category);

        // Collect IDs from this submission
        String newIds = answers.stream()
                .map(a -> String.valueOf(a.getQuestionId()))
                .collect(Collectors.joining(","));

        if (existing.isPresent()) {
            StudentQuestionHistory history = existing.get();
            String current = history.getSeenQuestionIds();
            if (current == null || current.isBlank()) {
                history.setSeenQuestionIds(newIds);
            } else {
                history.setSeenQuestionIds(current + "," + newIds);
            }
            historyRepository.save(history);
        } else {
            StudentQuestionHistory history = new StudentQuestionHistory();
            history.setCandidateName(candidateName);
            history.setCategory(category);
            history.setSeenQuestionIds(newIds);
            historyRepository.save(history);
        }
    }
}
