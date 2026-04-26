package com.example.sachitech.repository;

import com.example.sachitech.entity.LmsContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LmsContentRepository extends JpaRepository<LmsContent, Long> {

    List<LmsContent> findByCourseId(Long courseId);

    List<LmsContent> findByInternshipId(Long internshipId);

    List<LmsContent> findByCourseIdAndType(Long courseId, LmsContent.ContentType type);

    List<LmsContent> findByInternshipIdAndType(Long internshipId, LmsContent.ContentType type);

    List<LmsContent> findByUploadedById(Long userId);

    /**
     * Fetch content for courses the student is enrolled in.
     */
    @Query("SELECT c FROM LmsContent c WHERE c.course.id IN " +
           "(SELECT ec.id FROM StudentProfile sp JOIN sp.enrolledCourses ec WHERE sp.id = :studentId)")
    List<LmsContent> findContentForEnrolledCourses(@Param("studentId") Long studentId);

    /**
     * Fetch content for internships the student is enrolled in.
     */
    @Query("SELECT c FROM LmsContent c WHERE c.internship.id IN " +
           "(SELECT ei.id FROM StudentProfile sp JOIN sp.enrolledInternships ei WHERE sp.id = :studentId)")
    List<LmsContent> findContentForEnrolledInternships(@Param("studentId") Long studentId);

    /**
     * Fetch content by courseId only if student is enrolled in that course.
     */
    @Query("SELECT c FROM LmsContent c WHERE c.course.id = :courseId AND c.course.id IN " +
           "(SELECT ec.id FROM StudentProfile sp JOIN sp.enrolledCourses ec WHERE sp.id = :studentId)")
    List<LmsContent> findByCourseIdForEnrolledStudent(@Param("courseId") Long courseId,
                                                       @Param("studentId") Long studentId);
}
