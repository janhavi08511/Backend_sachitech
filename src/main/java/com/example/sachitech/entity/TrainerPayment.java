package com.example.sachitech.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "trainer_payment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainerPayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "trainer_id", nullable = false)
    private TrainerProfile trainer;
    
    @Column(nullable = false)
    private Double amount;
    
    @Column(nullable = false)
    private String paymentMode; // CASH, ONLINE, CHEQUE
    
    @Column(unique = true, nullable = false)
    private String paymentReference; // Unique reference number
    
    @Column(nullable = false)
    private LocalDate paymentDate;
    
    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, CANCELLED
    
    @Column(length = 500)
    private String remarks;
    
    @Column(nullable = false, updatable = false)
    private LocalDate createdAt;
    
    @Column(nullable = false)
    private LocalDate updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
        updatedAt = LocalDate.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDate.now();
    }
}
