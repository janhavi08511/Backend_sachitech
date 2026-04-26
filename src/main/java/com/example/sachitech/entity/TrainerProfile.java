package com.example.sachitech.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    private User user;

    private String phone;
    private String specialization;

    @JsonIgnore
    @OneToMany(mappedBy = "trainer", cascade = CascadeType.DETACH)
    private List<Course> coursesTaught;

    @JsonIgnore
    @OneToMany(mappedBy = "trainer", cascade = CascadeType.DETACH)
    private List<Internship> internshipsTaught;
    @OneToMany
    private List<Course> courses;
}
