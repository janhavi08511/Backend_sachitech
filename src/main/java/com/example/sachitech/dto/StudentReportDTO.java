package com.example.sachitech.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row per month for the "Student Admissions by Month" bar chart.
 * Fields match the Recharts dataKey names used in SuperAdminDashboard.tsx:
 *   dataKey="enrolled"  → total admissions that month
 *   dataKey="active"    → students whose feePaid == true (proxy for active)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentReportDTO {
    private String month;    // e.g. "Jan", "Feb"
    private long enrolled;   // admissions in that month
    private long active;     // feePaid == true in that month (approximation)
}
