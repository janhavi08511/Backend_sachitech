package com.example.sachitech.repository;

import com.example.sachitech.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByCourseId(Long courseId);
    List<Assignment> findByStudentId(Long studentId);
    List<Assignment> findByStudentIdAndCourseId(Long studentId, Long courseId);
}
