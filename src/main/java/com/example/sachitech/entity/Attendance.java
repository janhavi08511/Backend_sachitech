package com.example.sachitech.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "attendance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id", "date"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "enrolledCourses", "enrolledInternships"})
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User markedBy;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    /** Optimistic locking — prevents concurrent attendance overwrites */
    @Version
    @Column(name = "version")
    private Long version;

    public enum AttendanceStatus {
        PRESENT, ABSENT, LATE
    }
}
