package com.online.exam.service;

import com.online.exam.entity.Question;
import com.online.exam.entity.StudentQuestionHistory;
import com.online.exam.model.ExamStartRequest;
import com.online.exam.model.ExamStartResponse;
import com.online.exam.model.QuestionDTO;
import com.online.exam.repository.QuestionRepository;
import com.online.exam.repository.StudentQuestionHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final StudentQuestionHistoryRepository historyRepository;
    private final SessionService sessionService;

    @Autowired
    public QuestionService(QuestionRepository questionRepository,
                           StudentQuestionHistoryRepository historyRepository,
                           SessionService sessionService) {
        this.questionRepository = questionRepository;
        this.historyRepository = historyRepository;
        this.sessionService = sessionService;
    }

    public Question createQuestion(Question question) {
        return questionRepository.save(question);
    }

    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    public List<Question> getQuestionsByCategory(String category) {
        // Use Java Streams to filter questions by category from the general list
        return questionRepository.findAll().stream()
                .filter(q -> q.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    /**
     * Legacy endpoint kept for compatibility — returns shuffled questions without
     * session tracking or history filtering.
     */
    public List<QuestionDTO> getQuestionsForExam(String category) {
        List<QuestionDTO> dtos = questionRepository.findByCategoryIgnoreCase(category).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        Collections.shuffle(dtos);
        return dtos;
    }

    /**
     * Main exam-start flow:
     *  1. Block if the candidate already has an active session.
     *  2. Load question history for this candidate+category.
     *  3. Filter out already-seen questions.
     *  4. If none remain, auto-reset the history and serve all questions.
     *  5. Shuffle and return the unseen set; register the active session.
     *
     * @return ExamStartResponse with shuffled unseen questions and a wasReset flag,
     *         or null if the candidate already has an active session (caller returns 409).
     */
    public ExamStartResponse startExamSession(ExamStartRequest request) {
        String name = request.getCandidateName().trim();
        String category = request.getCategory().trim();

        // 1. Enforce single active session per candidate+category
        if (!sessionService.startSession(name, category)) {
            return null; // signal 409 to controller
        }

        // 2. Fetch all questions for this category
        List<Question> allQuestions = questionRepository.findByCategoryIgnoreCase(category);

        // 3. Load previously seen question IDs from DB
        Optional<StudentQuestionHistory> historyOpt =
                historyRepository.findByCandidateNameIgnoreCaseAndCategoryIgnoreCase(name, category);

        Set<Long> seenIds = new HashSet<>();
        if (historyOpt.isPresent() && historyOpt.get().getSeenQuestionIds() != null
                && !historyOpt.get().getSeenQuestionIds().isBlank()) {
            Arrays.stream(historyOpt.get().getSeenQuestionIds().split(","))
                  .map(String::trim)
                  .filter(s -> !s.isEmpty())
                  .map(Long::parseLong)
                  .forEach(seenIds::add);
        }

        // 4. Filter to unseen questions
        List<Question> unseen = allQuestions.stream()
                .filter(q -> !seenIds.contains(q.getId()))
                .collect(Collectors.toList());

        boolean wasReset = false;
        if (unseen.isEmpty()) {
            // All questions have been seen — reset history for this candidate+category
            historyOpt.ifPresent(historyRepository::delete);
            unseen = new ArrayList<>(allQuestions);
            wasReset = true;
        }

        // 5. Shuffle and convert to DTOs
        Collections.shuffle(unseen);
        List<QuestionDTO> dtos = unseen.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new ExamStartResponse(dtos, wasReset);
    }

    public Optional<Question> getQuestionById(Long id) {
        return questionRepository.findById(id);
    }

    public Optional<Question> updateQuestion(Long id, Question updatedQuestion) {
        return questionRepository.findById(id).map(existing -> {
            existing.setQuestionText(updatedQuestion.getQuestionText());
            existing.setOptionA(updatedQuestion.getOptionA());
            existing.setOptionB(updatedQuestion.getOptionB());
            existing.setOptionC(updatedQuestion.getOptionC());
            existing.setOptionD(updatedQuestion.getOptionD());
            existing.setCorrectOptionIndex(updatedQuestion.getCorrectOptionIndex());
            existing.setCategory(updatedQuestion.getCategory());
            return questionRepository.save(existing);
        });
    }

    public boolean deleteQuestion(Long id) {
        return questionRepository.findById(id).map(question -> {
            questionRepository.delete(question);
            return true;
        }).orElse(false);
    }

    private QuestionDTO convertToDTO(Question q) {
        List<String> options = List.of(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD());
        return new QuestionDTO(q.getId(), q.getQuestionText(), options, q.getCategory());
    }
}
