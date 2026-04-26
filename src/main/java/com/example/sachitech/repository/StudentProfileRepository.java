package com.example.sachitech.repository;

import com.example.sachitech.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);

    /**
     * Admissions grouped by month for a given year.
     * Returns Object[] rows: [monthNumber(1-12), monthName, count, activeCount]
     */
    @Query(value = """
        SELECT
            MONTH(s.admission_date)                          AS monthNum,
            DATE_FORMAT(s.admission_date, '%b')              AS monthName,
            COUNT(s.id)                                      AS enrolled,
            SUM(CASE WHEN s.fee_paid = 1 THEN 1 ELSE 0 END) AS active
        FROM student_profile s
        WHERE s.admission_date IS NOT NULL
          AND YEAR(s.admission_date) = :year
        GROUP BY MONTH(s.admission_date), DATE_FORMAT(s.admission_date, '%b')
        ORDER BY MONTH(s.admission_date)
        """, nativeQuery = true)
    List<Object[]> findAdmissionsByMonth(@Param("year") int year);

    /**
     * New admissions in a specific month/year — used in the downloadable report.
     */
    @Query(value = """
        SELECT s.* FROM student_profile s
        WHERE s.admission_date IS NOT NULL
          AND YEAR(s.admission_date) = :year
          AND MONTH(s.admission_date) = :month
        """, nativeQuery = true)
    List<StudentProfile> findByAdmissionMonthAndYear(@Param("month") int month, @Param("year") int year);
}
