package com.example.sachitech.repository;

import com.example.sachitech.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByStudentId(Long studentId);

    List<Attendance> findByCourseId(Long courseId);

    List<Attendance> findByCourseIdAndDate(Long courseId, LocalDate date);

    List<Attendance> findByStudentIdAndCourseId(Long studentId, Long courseId);

    Optional<Attendance> findByStudentIdAndCourseIdAndDate(Long studentId, Long courseId, LocalDate date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.course.id = :courseId AND a.status = 'PRESENT'")
    long countPresentByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.course.id = :courseId")
    long countTotalByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.status = 'PRESENT'")
    long countPresentByStudent(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId")
    long countTotalByStudent(@Param("studentId") Long studentId);

    @Query("SELECT DISTINCT a.date FROM Attendance a WHERE a.course.id = :courseId ORDER BY a.date DESC")
    List<LocalDate> findDistinctDatesByCourse(@Param("courseId") Long courseId);
}
