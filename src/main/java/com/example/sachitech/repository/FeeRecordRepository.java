package com.example.sachitech.repository;

import com.example.sachitech.entity.FeeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeeRecordRepository extends JpaRepository<FeeRecord, Long> {

    List<FeeRecord> findByStudentId(Long studentId);

    List<FeeRecord> findByCourseId(Long courseId);

    Optional<FeeRecord> findByStudentIdAndCourseId(Long studentId, Long courseId);

    /** Used by admin summary: sum of all enrolled fees */
    @Query("SELECT COALESCE(SUM(f.totalFeeAtEnrollment), 0) FROM FeeRecord f")
    Double sumTotalExpectedRevenue();

    /** Sum of all payments collected */
    @Query("SELECT COALESCE(SUM(f.amountPaid), 0) FROM FeeRecord f")
    Double sumTotalCollected();

    /** Sum of all pending amounts */
    @Query("SELECT COALESCE(SUM(f.pendingAmount), 0) FROM FeeRecord f")
    Double sumTotalPending();

    /** All records where student name matches (for search) */
    @Query("SELECT f FROM FeeRecord f WHERE LOWER(f.student.user.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<FeeRecord> findByStudentNameContaining(@Param("name") String name);
}
