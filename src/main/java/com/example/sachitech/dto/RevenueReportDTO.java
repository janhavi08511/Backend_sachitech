package com.example.sachitech.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row per month for the "Revenue Trend" area chart.
 * Fields match the Recharts dataKey names:
 *   dataKey="collected"  → total payments collected that month
 *   dataKey="pending"    → cumulative pending at end of that month (approximated
 *                          as totalExpected - collected for the month)
 *   dataKey="trainerPaid" → total trainer payments that month
 *   dataKey="profit"     → profit/loss for that month (collected - trainerPaid)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportDTO {
    private String month;           // e.g. "Jan"
    private double collected;       // SUM(installment_amount) for that month
    private double pending;         // totalExpected - collected (month-level estimate)
    private double trainerPaid;     // SUM(trainer_payments) for that month
    private double profit;          // collected - trainerPaid (profit/loss for month)
}
