package com.example.sachitech.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    // Concurrency lock: prevents two users marking same batch simultaneously
    @Column(name = "is_marking_active", nullable = false, columnDefinition = "boolean default false")
    private boolean markingActive = false;

    @Column(name = "marking_locked_by")
    private Long markingLockedBy; // userId who locked it

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internship_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Internship internship;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User trainer;
}
