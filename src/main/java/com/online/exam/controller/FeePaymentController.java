package com.online.exam.controller;

import com.online.exam.model.ArrearStudentDTO;
import com.online.exam.service.FeePaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fees")
@CrossOrigin(origins = "*")
public class FeePaymentController {

    private final FeePaymentService feePaymentService;

    @Autowired
    public FeePaymentController(FeePaymentService feePaymentService) {
        this.feePaymentService = feePaymentService;
    }

    /**
     * Called by the student from the payment modal on the login page.
     * Marks the fee as paid so the student can start a fresh exam session.
     */
    @PostMapping("/pay/{feeId}")
    public ResponseEntity<?> payFee(@PathVariable Long feeId) {
        return feePaymentService.payFee(feeId)
                .map(fee -> ResponseEntity.ok(
                        Map.of("message", "Fee paid successfully", "feeId", fee.getId())))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Admin-only endpoint — returns all students who have an arrear result,
     * together with their fee payment status.
     */
    @GetMapping("/arrears")
    public List<ArrearStudentDTO> getArrearStudents() {
        return feePaymentService.getAllArrearStudents();
    }
}
