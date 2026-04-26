package com.example.sachitech.repository;

import com.example.sachitech.entity.Internship;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InternshipRepository extends JpaRepository<Internship, Long> {

    /** Count internships with status = 'ACTIVE' (case-insensitive) */
    long countByStatusIgnoreCase(String status);
}
