package com.online.exam.service;

import com.online.exam.entity.ExamResult;
import com.online.exam.entity.FeePayment;
import com.online.exam.model.ArrearStudentDTO;
import com.online.exam.repository.ExamResultRepository;
import com.online.exam.repository.FeePaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeePaymentService {

    /** Re-exam fee amount in INR. */
    public static final double RE_EXAM_FEE = 500.0;

    private final FeePaymentRepository feePaymentRepository;
    private final ExamResultRepository examResultRepository;

    @Autowired
    public FeePaymentService(FeePaymentRepository feePaymentRepository,
                             ExamResultRepository examResultRepository) {
        this.feePaymentRepository = feePaymentRepository;
        this.examResultRepository = examResultRepository;
    }

    /**
     * Creates an unpaid re-exam fee for a student who scored below 50%.
     * If an unpaid fee already exists for this candidate+category, no duplicate is created.
     */
    public FeePayment createFeeForArrear(String candidateName, String category) {
        Optional<FeePayment> existing = feePaymentRepository
                .findByCandidateNameIgnoreCaseAndCategoryIgnoreCaseAndPaidFalse(
                        candidateName, category);
        if (existing.isPresent()) {
            return existing.get(); // idempotent — don't double-charge
        }
        FeePayment fee = new FeePayment();
        fee.setCandidateName(candidateName);
        fee.setCategory(category);
        fee.setAmount(RE_EXAM_FEE);
        fee.setPaid(false);
        fee.setCreatedAt(LocalDateTime.now());
        return feePaymentRepository.save(fee);
    }

    /**
     * Returns true when this candidate has an unpaid re-exam fee for the given category.
     * Used to gate entry via HTTP 402.
     */
    public boolean hasUnpaidFee(String candidateName, String category) {
        return feePaymentRepository
                .findByCandidateNameIgnoreCaseAndCategoryIgnoreCaseAndPaidFalse(
                        candidateName, category)
                .isPresent();
    }

    /**
     * Returns the unpaid FeePayment for this candidate+category, or empty if none.
     */
    public Optional<FeePayment> getUnpaidFee(String candidateName, String category) {
        return feePaymentRepository
                .findByCandidateNameIgnoreCaseAndCategoryIgnoreCaseAndPaidFalse(
                        candidateName, category);
    }

    /**
     * Marks a fee as paid. Returns the updated FeePayment, or empty if ID not found.
     */
    public Optional<FeePayment> payFee(Long feeId) {
        return feePaymentRepository.findById(feeId).map(fee -> {
            fee.setPaid(true);
            fee.setPaidAt(LocalDateTime.now());
            return feePaymentRepository.save(fee);
        });
    }

    /**
     * Builds the admin Arrear Students List by:
     * 1. Finding all ExamResult records where arrear = true.
     * 2. Grouping by candidate+category, keeping only the latest result per pair.
     * 3. Joining with fee payment status.
     */
    public List<ArrearStudentDTO> getAllArrearStudents() {
        // Fetch all arrear results
        List<ExamResult> arrearResults = examResultRepository.findAll().stream()
                .filter(ExamResult::isArrear)
                .collect(Collectors.toList());

        // Keep only the latest result per candidate+category
        Map<String, ExamResult> latestPerPair = new LinkedHashMap<>();
        for (ExamResult r : arrearResults) {
            String key = r.getCandidateName().toLowerCase() + "::" + r.getCategory().toLowerCase();
            ExamResult existing = latestPerPair.get(key);
            if (existing == null || r.getSubmissionTime().isAfter(existing.getSubmissionTime())) {
                latestPerPair.put(key, r);
            }
        }

        // Fetch all unpaid fees for quick lookup
        Map<String, FeePayment> unpaidFeeMap = feePaymentRepository.findAllByPaidFalse().stream()
                .collect(Collectors.toMap(
                        f -> f.getCandidateName().toLowerCase() + "::" + f.getCategory().toLowerCase(),
                        f -> f,
                        (a, b) -> a // keep first if duplicates
                ));

        // Build DTOs
        return latestPerPair.values().stream().map(result -> {
            String key = result.getCandidateName().toLowerCase() + "::" + result.getCategory().toLowerCase();
            FeePayment fee = unpaidFeeMap.get(key);
            boolean feePaid = (fee == null); // if no unpaid fee exists → already paid or cleared
            Long feeId = (fee != null) ? fee.getId() : null;
            double feeAmount = (fee != null) ? fee.getAmount() : RE_EXAM_FEE;

            return new ArrearStudentDTO(
                    result.getCandidateName(),
                    result.getCategory(),
                    result.getScore(),
                    result.getTotalQuestions(),
                    result.getPercentage(),
                    result.getSubmissionTime(),
                    feePaid,
                    feeId,
                    feeAmount
            );
        }).collect(Collectors.toList());
    }
}
