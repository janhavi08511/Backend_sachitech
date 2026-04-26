package com.example.sachitech.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * One FeeRecord per student-course enrollment.
 * Stores the snapshot of the fee at enrollment time and tracks running totals.
 */
@Entity
@Table(name = "fee_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_fee_record_student"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "enrolledCourses", "enrolledInternships"})
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id",
                foreignKey = @ForeignKey(name = "fk_fee_record_course"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Course course;

    /** Snapshot of course fee at the time of enrollment — immutable after creation */
    @Column(name = "total_fee_at_enrollment", nullable = false)
    private Double totalFeeAtEnrollment;

    /** Running total of all payments received */
    @Column(name = "amount_paid", nullable = false)
    private Double amountPaid = 0.0;

    /** Auto-calculated: totalFeeAtEnrollment - amountPaid */
    @Column(name = "pending_amount", nullable = false)
    private Double pendingAmount;

    @Column(name = "last_transaction_date")
    private LocalDate lastTransactionDate;

    /** Optimistic locking — prevents concurrent fee updates causing dirty writes */
    @Version
    @Column(name = "version")
    private Long version;
}
