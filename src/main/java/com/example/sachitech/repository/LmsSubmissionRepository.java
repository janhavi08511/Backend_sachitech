package com.example.sachitech.repository;

import com.example.sachitech.entity.LmsSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LmsSubmissionRepository extends JpaRepository<LmsSubmission, Long> {

    List<LmsSubmission> findByStudentId(Long studentId);

    List<LmsSubmission> findByAssignmentId(Long assignmentId);

    Optional<LmsSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    List<LmsSubmission> findByAssignment_Course_Id(Long courseId);
}
