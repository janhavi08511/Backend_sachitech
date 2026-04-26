package com.example.sachitech.repository;

import com.example.sachitech.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByCourseId(Long courseId);
}
