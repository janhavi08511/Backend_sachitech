package com.example.sachitech.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row per course for the "Course-wise Revenue Analysis" table and
 * the "Course Distribution" pie chart.
 *
 * Fields match the column accessors in SuperAdminDashboard.tsx:
 *   c.courseName, c.enrolled, c.courseFee,
 *   c.expectedRevenue, c.collectedRevenue, c.pendingRevenue
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseReportDTO {
    private Long   courseId;
    private String courseName;
    private double courseFee;           // Course.totalFee
    private long   enrolled;            // count of FeeRecord rows for this course
    private double expectedRevenue;     // SUM(total_fee_at_enrollment)
    private double collectedRevenue;    // SUM(amount_paid)
    private double pendingRevenue;      // SUM(pending_amount)
}
