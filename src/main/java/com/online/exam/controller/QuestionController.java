package com.online.exam.controller;

import com.online.exam.entity.FeePayment;
import com.online.exam.model.ExamStartRequest;
import com.online.exam.model.ExamStartResponse;
import com.online.exam.model.QuestionDTO;
import com.online.exam.entity.Question;
import com.online.exam.service.FeePaymentService;
import com.online.exam.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*")
public class QuestionController {

    private final QuestionService questionService;
    private final FeePaymentService feePaymentService;

    @Autowired
    public QuestionController(QuestionService questionService,
                              FeePaymentService feePaymentService) {
        this.questionService = questionService;
        this.feePaymentService = feePaymentService;
    }

    @GetMapping
    public List<Question> getAllQuestions() {
        return questionService.getAllQuestions();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        return questionService.getQuestionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/exam")
    public List<QuestionDTO> getQuestionsForExam(@RequestParam(defaultValue = "java") String category) {
        return questionService.getQuestionsForExam(category);
    }

    /**
     * Starts a new exam session for a candidate.
     * Returns HTTP 402 PAYMENT_REQUIRED if the student has an unpaid arrear re-exam fee.
     * Returns HTTP 409 CONFLICT if the candidate already has an active session.
     */
    @PostMapping("/start")
    public ResponseEntity<?> startExam(@RequestBody ExamStartRequest request) {
        String name = request.getCandidateName() != null ? request.getCandidateName().trim() : "";
        String category = request.getCategory() != null ? request.getCategory().trim() : "";

        // Check for unpaid arrear fee first
        Optional<FeePayment> unpaidFee = feePaymentService.getUnpaidFee(name, category);
        if (unpaidFee.isPresent()) {
            FeePayment fee = unpaidFee.get();
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(
                    Map.of("feeId", fee.getId(),
                           "feeAmount", fee.getAmount(),
                           "message", "Re-exam fee payment required before starting exam")
            );
        }

        ExamStartResponse response = questionService.startExamSession(request);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public Question createQuestion(@RequestBody Question question) {
        return questionService.createQuestion(question);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @RequestBody Question question) {
        return questionService.updateQuestion(id, question)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        if (questionService.deleteQuestion(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
