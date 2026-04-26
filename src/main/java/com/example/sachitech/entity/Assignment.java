package com.example.sachitech.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Double maxScore;
    
    // Linking to Course and Student for submission/evaluation context
    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonIgnore
    private Course course;

    @ManyToOne
    @JoinColumn(name = "internship_id")
    @JsonIgnore
    private Internship internship;

    @ManyToOne
    @JoinColumn(name = "student_id")
    @JsonIgnore
    private StudentProfile student;

    private String studentSubmissionLink;
    private Double score;
    private String evaluationStatus;
}
