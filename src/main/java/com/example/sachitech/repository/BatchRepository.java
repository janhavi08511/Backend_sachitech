package com.example.sachitech.repository;

import com.example.sachitech.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {
    List<Batch> findByTrainerId(Long trainerId);
    List<Batch> findByCourseId(Long courseId);
    List<Batch> findByInternshipId(Long internshipId);
    List<Batch> findByTrainerIdAndCourseId(Long trainerId, Long courseId);
    List<Batch> findByTrainerIdAndInternshipId(Long trainerId, Long internshipId);
}
