package com.online.exam.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Records a re-exam fee charged to a student who scored below 50% (arrear).
 * A fee record is automatically created when an arrear result is saved,
 * and is marked paid when the student completes payment before re-attempting.
 */
@Entity
@Table(name = "fee_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_name", nullable = false)
    private String candidateName;

    @Column(nullable = false)
    private String category;

    /** Fee amount in INR (default ₹500). */
    @Column(nullable = false)
    private double amount;

    /** False until the student pays; flipped to true via POST /api/fees/pay/{id}. */
    @Column(nullable = false)
    private boolean paid;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
