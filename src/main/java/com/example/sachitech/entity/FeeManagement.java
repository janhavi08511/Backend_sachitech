package com.example.sachitech.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeManagement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    @JsonIgnore
    private StudentProfile student;

    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonIgnore
    private Course course;

    @ManyToOne
    @JoinColumn(name = "internship_id")
    @JsonIgnore
    private Internship internship;

    private Double feeAmount;
    private Double pendingAmount;
    private LocalDate paymentDate;
    private String paymentType;
}
