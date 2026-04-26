package com.example.sachitech.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchDTO {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long courseId;
    private Long internshipId;
    private Long trainerId;
}
