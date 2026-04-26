package com.example.sachitech.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lms_content")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LmsContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType type; // ASSIGNMENT or NOTE

    @Column(nullable = false)
    private String fileUrl; // local path / filename

    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonIgnore
    private Course course;

    @ManyToOne
    @JoinColumn(name = "internship_id")
    @JsonIgnore
    private Internship internship;

    @Column(nullable = false)
    private LocalDateTime uploadDate;

    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    @JsonIgnore
    private User uploadedBy;

    public enum ContentType {
        ASSIGNMENT, NOTE
    }
}
