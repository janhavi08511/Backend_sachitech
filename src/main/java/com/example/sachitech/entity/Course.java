package com.example.sachitech.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String duration;
    private String category;
    private String description;
    private Double totalFee;
    private String status;
    private String prerequisite;
    private String progress;

    // Concurrency lock for attendance marking
    @Column(name = "is_marking_active", nullable = false, columnDefinition = "boolean default false")
    private boolean markingActive = false;

    @Column(name = "marking_locked_by")
    private Long markingLockedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private TrainerProfile trainer;
}
