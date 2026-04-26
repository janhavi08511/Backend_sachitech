package com.example.sachitech.repository;

import com.example.sachitech.entity.TrainerPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TrainerPaymentRepository extends JpaRepository<TrainerPayment, Long> {
    
    /**
     * Find all payments for a specific trainer
     */
    List<TrainerPayment> findByTrainerId(Long trainerId);
    
    /**
     * Find all payments for a trainer within a date range
     */
    List<TrainerPayment> findByTrainerIdAndPaymentDateBetween(Long trainerId, LocalDate from, LocalDate to);
    
    /**
     * Find all payments with a specific status
     */
    List<TrainerPayment> findByStatus(String status);
    
    /**
     * Find all completed payments for a trainer
     */
    @Query("SELECT tp FROM TrainerPayment tp WHERE tp.trainer.id = :trainerId AND tp.status = 'COMPLETED'")
    List<TrainerPayment> findCompletedPaymentsForTrainer(@Param("trainerId") Long trainerId);
    
    /**
     * Sum total amount paid to a specific trainer
     */
    @Query("SELECT COALESCE(SUM(tp.amount), 0.0) FROM TrainerPayment tp WHERE tp.trainer.id = :trainerId AND tp.status = 'COMPLETED'")
    Double sumTotalPaidToTrainer(@Param("trainerId") Long trainerId);
    
    /**
     * Sum total amount paid to all trainers
     */
    @Query("SELECT COALESCE(SUM(tp.amount), 0.0) FROM TrainerPayment tp WHERE tp.status = 'COMPLETED'")
    Double sumTotalPaidToAllTrainers();
    
    /**
     * Find payments by month and year
     */
    @Query("SELECT tp FROM TrainerPayment tp WHERE YEAR(tp.paymentDate) = :year AND MONTH(tp.paymentDate) = :month AND tp.status = 'COMPLETED'")
    List<TrainerPayment> findByMonthAndYear(@Param("month") int month, @Param("year") int year);
    
    /**
     * Sum payments for a specific month and year
     */
    @Query("SELECT COALESCE(SUM(tp.amount), 0.0) FROM TrainerPayment tp WHERE YEAR(tp.paymentDate) = :year AND MONTH(tp.paymentDate) = :month AND tp.status = 'COMPLETED'")
    Double sumPaidInMonth(@Param("month") int month, @Param("year") int year);
}
