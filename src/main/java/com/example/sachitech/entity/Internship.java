package com.example.sachitech.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Internship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String duration;
    private String category;
    private Double totalFee;
    private String status;
    private String prerequisite;
    private String progress;

    @ManyToOne
    @JoinColumn(name = "trainer_id")
    @JsonIgnore
    private TrainerProfile trainer;
}
