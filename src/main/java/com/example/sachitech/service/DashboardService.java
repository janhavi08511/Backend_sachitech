package com.example.sachitech.service;

import com.example.sachitech.dto.CourseReportDTO;
import com.example.sachitech.dto.DashboardSummaryDTO;
import com.example.sachitech.dto.RevenueReportDTO;
import com.example.sachitech.dto.StudentReportDTO;
import com.example.sachitech.entity.Role;
import com.example.sachitech.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository            userRepository;
    private final CourseRepository          courseRepository;
    private final InternshipRepository      internshipRepository;
    private final FeeRecordRepository       feeRecordRepository;
    private final FeeTransactionRepository  feeTransactionRepository;
    private final StudentProfileRepository  studentProfileRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Summary card — cached for 5 minutes, evicted on schedule
    // ─────────────────────────────────────────────────────────────────────────

    @Cacheable("dashboardSummary")
    public DashboardSummaryDTO getSummary() {
        LocalDate now = LocalDate.now();

        long   totalStudents     = userRepository.countByRole(Role.STUDENT);
        long   totalCourses      = courseRepository.countByStatusIgnoreCase("ACTIVE");
        long   ongoingInternships= internshipRepository.countByStatusIgnoreCase("ACTIVE");
        long   totalPlacements   = internshipRepository.count(); // all internships = placement opportunities
        double totalRevenue      = feeRecordRepository.sumTotalCollected();
        double totalPending      = feeRecordRepository.sumTotalPending();
        double totalExpected     = feeRecordRepository.sumTotalExpectedRevenue();
        double thisMonthRevenue  = feeTransactionRepository.sumCollectedInMonth(now.getYear(), now.getMonthValue());
        long   totalPayments     = feeTransactionRepository.countAllTransactions();

        return DashboardSummaryDTO.builder()
                .totalStudents(totalStudents)
                .totalCourses(totalCourses)
                .totalPlacements(totalPlacements)
                .ongoingInternships(ongoingInternships)
                .totalRevenue(totalRevenue)
                .totalPending(totalPending)
                .totalExpected(totalExpected)
                .thisMonthRevenue(thisMonthRevenue)
                .totalPayments(totalPayments)
                .build();
    }

    /** Evict the summary cache every 5 minutes so data stays fresh. */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @CacheEvict(value = "dashboardSummary", allEntries = true)
    public void evictSummaryCache() {
        // Spring handles the eviction; nothing to do here
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Student admissions by month (current year)
    // ─────────────────────────────────────────────────────────────────────────

    @Cacheable("studentReport")
    public List<StudentReportDTO> getStudentReport() {
        int year = LocalDate.now().getYear();
        List<Object[]> rows = studentProfileRepository.findAdmissionsByMonth(year);
        return rows.stream().map(r -> new StudentReportDTO(
                (String)  r[1],                          // monthName
                ((Number) r[2]).longValue(),              // enrolled
                ((Number) r[3]).longValue()               // active (feePaid)
        )).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Revenue trend by month (current year)
    // ─────────────────────────────────────────────────────────────────────────

    @Cacheable("revenueReport")
    public List<RevenueReportDTO> getRevenueReport() {
        int    year         = LocalDate.now().getYear();
        double totalExpected = feeRecordRepository.sumTotalExpectedRevenue();
        List<Object[]> rows = feeTransactionRepository.findMonthlyRevenue(year);

        return rows.stream().map(r -> {
            double collected = ((Number) r[2]).doubleValue();
            // Pending is approximated as (totalExpected / 12) - collected for the month
            // This gives a meaningful trend line even without per-month expected data
            double monthlyExpected = totalExpected / 12.0;
            double pending = Math.max(0, monthlyExpected - collected);
            // trainerPaid and profit are 0 for now (can be enhanced later)
            return new RevenueReportDTO((String) r[1], collected, pending, 0.0, collected);
        }).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Course-wise revenue breakdown
    // ─────────────────────────────────────────────────────────────────────────

    @Cacheable("courseReport")
    public List<CourseReportDTO> getCourseReport() {
        List<Object[]> rows = courseRepository.findCourseRevenueBreakdown();
        return rows.stream().map(r -> new CourseReportDTO(
                ((Number) r[0]).longValue(),    // courseId
                (String)  r[1],                 // courseName
                ((Number) r[2]).doubleValue(),   // courseFee
                ((Number) r[3]).longValue(),     // enrolled
                ((Number) r[4]).doubleValue(),   // expectedRevenue
                ((Number) r[5]).doubleValue(),   // collectedRevenue
                ((Number) r[6]).doubleValue()    // pendingRevenue
        )).toList();
    }
}
