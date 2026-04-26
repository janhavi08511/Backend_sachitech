package com.example.sachitech.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned by GET /reports/summary
 * Maps directly to the summary cards in the SuperAdmin dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {

    // ── Student cards ──────────────────────────────────────────────────────────
    private long totalStudents;
    private long totalCourses;
    private long totalPlacements;       // count of internship enrollments
    private long ongoingInternships;    // internships with status ACTIVE

    // ── Fee cards ──────────────────────────────────────────────────────────────
    private double totalRevenue;        // sum of all amount_paid
    private double totalPending;        // sum of all pending_amount
    private double totalExpected;       // sum of total_fee_at_enrollment
    private double thisMonthRevenue;    // payments collected in current month

    // ── Transaction card ──────────────────────────────────────────────────────
    private long totalPayments;         // count of all fee_transaction rows
}
