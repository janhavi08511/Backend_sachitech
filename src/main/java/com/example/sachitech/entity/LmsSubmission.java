package com.example.sachitech.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lms_submission")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LmsSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "assignment_id", nullable = false)
    @JsonIgnore
    private LmsContent assignment;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private StudentProfile student;

    private String fileUrl; // uploaded PDF filename

    private LocalDateTime submissionDate;

    @Enumerated(EnumType.STRING)
    private SubmissionStatus status; // PENDING, SUBMITTED, EVALUATED

    private Double score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    public enum SubmissionStatus {
        PENDING, SUBMITTED, EVALUATED
    }
}
