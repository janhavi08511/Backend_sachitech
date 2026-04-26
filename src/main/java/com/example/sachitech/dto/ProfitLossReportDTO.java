package com.example.sachitech.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Comprehensive profit/loss report for admin dashboard.
 * Shows financial health of the organization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfitLossReportDTO {
    private Double totalStudentFeesCollected;  // Total revenue from students
    private Double totalTrainerPayments;       // Total expenses (trainer payments)
    private Double profitLoss;                 // Net profit/loss
    private Double profitMargin;               // Profit margin percentage
    private Long totalStudents;                // Total active students
    private Long totalTrainers;                // Total trainers
    private Long totalTransactions;            // Total fee transactions
}
