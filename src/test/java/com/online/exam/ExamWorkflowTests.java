package com.online.exam;

import com.online.exam.entity.Question;
import com.online.exam.entity.StudentQuestionHistory;
import com.online.exam.model.AnswerSubmission;
import com.online.exam.model.ExamGradingResponse;
import com.online.exam.model.ExamStartRequest;
import com.online.exam.model.ExamStartResponse;
import com.online.exam.model.ExamSubmission;
import com.online.exam.repository.QuestionRepository;
import com.online.exam.repository.StudentQuestionHistoryRepository;
import com.online.exam.service.ExamResultService;
import com.online.exam.service.QuestionService;
import com.online.exam.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ExamWorkflowTests {

    @Autowired private QuestionService questionService;
    @Autowired private ExamResultService examResultService;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private StudentQuestionHistoryRepository historyRepository;
    @Autowired private SessionService sessionService;

    /** Clean up any lingering sessions and question history between tests. */
    @BeforeEach
    void cleanUp() {
        // Release any leftover active sessions used by test candidates
        sessionService.endSession("Alice", "java");
        sessionService.endSession("Bob", "java");
        sessionService.endSession("TestReset", "java");
        // Remove question history records left by previous test runs
        historyRepository.findByCandidateNameIgnoreCaseAndCategoryIgnoreCase("Alice", "java")
                .ifPresent(historyRepository::delete);
        historyRepository.findByCandidateNameIgnoreCaseAndCategoryIgnoreCase("TestReset", "java")
                .ifPresent(historyRepository::delete);
    }

    // -----------------------------------------------------------------------
    // 1. SHUFFLING: questions must vary across multiple fetches
    // -----------------------------------------------------------------------
    @Test
    void testQuestionsAreShuffledAcrossRequests() {
        String category = "java";
        List<Question> dbQuestions = questionRepository.findByCategoryIgnoreCase(category);
        assertTrue(dbQuestions.size() >= 3, "Need at least 3 seeded Java questions");

        List<List<Long>> orderList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            // Start a fresh session each iteration (requires ending it each time)
            String candidate = "Shuffle_Test_" + i;
            ExamStartResponse resp = questionService.startExamSession(
                    new ExamStartRequest(candidate, category));
            assertNotNull(resp, "Session should start successfully");
            orderList.add(resp.getQuestions().stream()
                    .map(q -> q.getId()).collect(Collectors.toList()));
            sessionService.endSession(candidate, category);
        }

        boolean orderChanged = false;
        List<Long> first = orderList.get(0);
        for (int i = 1; i < orderList.size(); i++) {
            if (!first.equals(orderList.get(i))) { orderChanged = true; break; }
        }
        assertTrue(orderChanged, "Question order must be randomized across requests");
    }

    // -----------------------------------------------------------------------
    // 2. SINGLE SESSION: same candidate cannot start twice concurrently
    // -----------------------------------------------------------------------
    @Test
    void testSingleActiveSessionPerCandidate() {
        ExamStartRequest req = new ExamStartRequest("Alice", "java");

        ExamStartResponse first = questionService.startExamSession(req);
        assertNotNull(first, "First session should start successfully");

        ExamStartResponse second = questionService.startExamSession(req);
        assertNull(second, "Second concurrent session must be blocked (null = 409)");

        // After ending the first session the candidate should be able to start again
        sessionService.endSession("Alice", "java");
        ExamStartResponse third = questionService.startExamSession(req);
        assertNotNull(third, "Session should succeed after previous session ends");
        sessionService.endSession("Alice", "java");
    }

    // -----------------------------------------------------------------------
    // 3. NO REPEATED QUESTIONS: each attempt excludes already-seen questions
    // -----------------------------------------------------------------------
    @Test
    void testNoRepeatedQuestionsAcrossAttempts() {
        String category = "java";
        List<Question> allQuestions = questionRepository.findByCategoryIgnoreCase(category);
        assertTrue(allQuestions.size() >= 2, "Need at least 2 Java questions");

        // --- Attempt 1 ---
        ExamStartResponse attempt1 = questionService.startExamSession(
                new ExamStartRequest("Alice", category));
        assertNotNull(attempt1);
        assertFalse(attempt1.isWasReset(), "First attempt should not trigger a reset");

        List<Long> attempt1Ids = attempt1.getQuestions().stream()
                .map(q -> q.getId()).collect(Collectors.toList());

        // Simulate submission with all questions answered
        List<AnswerSubmission> answers1 = attempt1.getQuestions().stream()
                .map(q -> new AnswerSubmission(q.getId(), 0))
                .collect(Collectors.toList());
        examResultService.gradeAndSaveSubmission(
                new ExamSubmission("Alice", category, answers1));
        // gradeAndSaveSubmission releases the session internally

        // Verify history was persisted
        Optional<StudentQuestionHistory> history =
                historyRepository.findByCandidateNameIgnoreCaseAndCategoryIgnoreCase("Alice", category);
        assertTrue(history.isPresent(), "Question history should be persisted after submission");

        // --- Attempt 2 (only remaining unseen questions served) ---
        ExamStartResponse attempt2 = questionService.startExamSession(
                new ExamStartRequest("Alice", category));
        assertNotNull(attempt2);
        sessionService.endSession("Alice", category); // don't need to grade for this check

        if (!attempt2.isWasReset()) {
            // If there were unseen questions remaining, none should overlap with attempt 1
            List<Long> attempt2Ids = attempt2.getQuestions().stream()
                    .map(q -> q.getId()).collect(Collectors.toList());
            boolean overlap = attempt2Ids.stream().anyMatch(attempt1Ids::contains);
            assertFalse(overlap, "Attempt 2 must not repeat any questions from attempt 1");
        }
        // If wasReset==true the DB only had the same questions — that's valid behaviour
    }

    // -----------------------------------------------------------------------
    // 4. AUTO-RESET: when all questions exhausted, history resets and wasReset=true
    // -----------------------------------------------------------------------
    @Test
    void testAutoResetWhenAllQuestionsExhausted() {
        String category = "java";
        List<Question> allQuestions = questionRepository.findByCategoryIgnoreCase(category);
        String candidate = "TestReset";

        // Manually insert a history record that marks ALL questions as seen
        String allIds = allQuestions.stream()
                .map(q -> String.valueOf(q.getId()))
                .collect(Collectors.joining(","));
        StudentQuestionHistory fakeHistory = new StudentQuestionHistory();
        fakeHistory.setCandidateName(candidate);
        fakeHistory.setCategory(category);
        fakeHistory.setSeenQuestionIds(allIds);
        historyRepository.save(fakeHistory);

        // Now start a session — should auto-reset and return all questions
        ExamStartResponse resp = questionService.startExamSession(
                new ExamStartRequest(candidate, category));

        assertNotNull(resp, "Session should start despite exhausted history");
        assertTrue(resp.isWasReset(), "wasReset must be true when all questions were exhausted");
        assertEquals(allQuestions.size(), resp.getQuestions().size(),
                "All questions returned after reset");
        sessionService.endSession(candidate, category);
    }

    // -----------------------------------------------------------------------
    // 5. GRADING ORDER: results follow the candidate's submitted question order
    // -----------------------------------------------------------------------
    @Test
    void testGradingPreservesSubmittedOrder() {
        String category = "java";
        ExamStartResponse start = questionService.startExamSession(
                new ExamStartRequest("Alice", category));
        assertNotNull(start);

        List<Question> dbQuestions = questionRepository.findAll();
        List<AnswerSubmission> answers = new ArrayList<>();

        for (int i = 0; i < start.getQuestions().size(); i++) {
            var qDto = start.getQuestions().get(i);
            Question dbQ = dbQuestions.stream()
                    .filter(q -> q.getId().equals(qDto.getId()))
                    .findFirst().orElseThrow();
            // First question correct, rest wrong
            int idx = (i == 0) ? dbQ.getCorrectOptionIndex() : (dbQ.getCorrectOptionIndex() + 1) % 4;
            answers.add(new AnswerSubmission(qDto.getId(), idx));
        }

        ExamGradingResponse response = examResultService.gradeAndSaveSubmission(
                new ExamSubmission("Alice", category, answers));
        // session released internally by gradeAndSaveSubmission

        assertEquals("Alice", response.getUserName());
        assertEquals(1, response.getScore());
        assertEquals(answers.size(), response.getQuestionResults().size());

        for (int i = 0; i < answers.size(); i++) {
            assertEquals(answers.get(i).getQuestionId(),
                    response.getQuestionResults().get(i).getQuestionId(),
                    "Result order must match submitted order");
            if (i == 0) assertTrue(response.getQuestionResults().get(i).isCorrect());
            else        assertFalse(response.getQuestionResults().get(i).isCorrect());
        }
    }
}
