package com.example.sachitech.repository;

import com.example.sachitech.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    /** Count courses with status = 'ACTIVE' (case-insensitive) */
    long countByStatusIgnoreCase(String status);

    /**
     * Course-wise revenue breakdown.
     * Returns Object[] rows:
     *   [courseId, courseName, courseFee, enrolled, expectedRevenue, collectedRevenue, pendingRevenue]
     */
    @Query("""
        SELECT
            c.id,
            c.name,
            COALESCE(c.totalFee, 0),
            COUNT(fr.id),
            COALESCE(SUM(fr.totalFeeAtEnrollment), 0),
            COALESCE(SUM(fr.amountPaid), 0),
            COALESCE(SUM(fr.pendingAmount), 0)
        FROM Course c
        LEFT JOIN FeeRecord fr ON fr.course.id = c.id
        GROUP BY c.id, c.name, c.totalFee
        ORDER BY COALESCE(SUM(fr.amountPaid), 0) DESC
        """)
    List<Object[]> findCourseRevenueBreakdown();
}
