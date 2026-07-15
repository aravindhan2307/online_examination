package com.online.exam.controller;

import com.online.exam.entity.ExamResult;
import com.online.exam.model.ExamGradingResponse;
import com.online.exam.model.ExamSubmission;
import com.online.exam.service.ExamResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/results")
@CrossOrigin(origins = "*")
public class ExamResultController {

    private final ExamResultService examResultService;

    @Autowired
    public ExamResultController(ExamResultService examResultService) {
        this.examResultService = examResultService;
    }

    @GetMapping
    public List<ExamResult> getAllResults() {
        return examResultService.getAllResults();
    }

    @PostMapping("/submit")
    public ExamGradingResponse submitExam(@RequestBody ExamSubmission submission) {
        return examResultService.gradeAndSaveSubmission(submission);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResult(@PathVariable Long id) {
        if (examResultService.deleteResult(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
