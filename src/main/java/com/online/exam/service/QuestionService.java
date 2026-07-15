package com.online.exam.service;

import com.online.exam.entity.Question;
import com.online.exam.model.QuestionDTO;
import com.online.exam.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    @Autowired
    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
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

    public List<QuestionDTO> getQuestionsForExam(String category) {
        // Retrieve and convert to DTO using stream mapping
        return questionRepository.findByCategoryIgnoreCase(category).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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
