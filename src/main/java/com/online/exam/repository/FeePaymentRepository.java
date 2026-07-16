package com.online.exam.repository;

import com.online.exam.entity.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {

    /** Find an unpaid fee for a specific candidate+category — used to gate exam entry. */
    Optional<FeePayment> findByCandidateNameIgnoreCaseAndCategoryIgnoreCaseAndPaidFalse(
            String candidateName, String category);

    /** All unpaid fees — used to build the admin arrear students list. */
    List<FeePayment> findAllByPaidFalse();

    /** All fees for a candidate across categories. */
    List<FeePayment> findByCandidateNameIgnoreCase(String candidateName);
}
