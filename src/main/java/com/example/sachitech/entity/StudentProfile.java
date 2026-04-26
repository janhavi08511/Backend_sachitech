package com.example.sachitech.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔥 FIX: Owning side of relationship
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    private LocalDate admissionDate;
    private Boolean feePaid;
    private String phone;
    private String course;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "student_courses",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> enrolledCourses;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "student_internships",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "internship_id")
    )
    private List<Internship> enrolledInternships;
}
