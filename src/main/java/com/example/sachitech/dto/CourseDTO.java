package com.example.sachitech.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private Long id;
    private String name;
    private String duration;
    private String category;
    private String description;
    private Double totalFee;
    private String status;
    private String prerequisite;
    private String progress;
}
