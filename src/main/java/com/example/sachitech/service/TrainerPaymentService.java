package com.example.sachitech.service;

import com.example.sachitech.entity.TrainerPayment;
import com.example.sachitech.entity.TrainerProfile;
import com.example.sachitech.repository.TrainerPaymentRepository;
import com.example.sachitech.repository.TrainerProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class TrainerPaymentService {

    @Autowired
    private TrainerPaymentRepository trainerPaymentRepository;

    @Autowired
    private TrainerProfileRepository trainerProfileRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // DTOs
    // ─────────────────────────────────────────────────────────────────────────

    public record TrainerPaymentDTO(
            Long paymentId,
            Long trainerId,
            String trainerName,
            Double amount,
            String paymentMode,
            String paymentReference,
            LocalDate paymentDate,
            String status,
            String remarks
    ) {}

    public record TrainerPaymentSummaryDTO(
            Long trainerId,
            String trainerName,
            Double totalPaid,
            Long completedPayments,
            Long pendingPayments,
            Double pendingAmount
    ) {}

    public record ProfitLossReportDTO(
            Double totalStudentFeesCollected,
            Double totalTrainerPayments,
            Double profitLoss,
            Double profitMargin,
            Long totalStudents,
            Long totalTrainers,
            Long totalTransactions
    ) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Create trainer payment
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public TrainerPaymentDTO recordPayment(Long trainerId, Double amount, String paymentMode, 
                                           LocalDate paymentDate, String remarks) {
        TrainerProfile trainer = trainerProfileRepository.findById(trainerId)
                .orElseThrow(() -> new RuntimeException("Trainer not found: " + trainerId));

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        TrainerPayment payment = new TrainerPayment();
        payment.setTrainer(trainer);
        payment.setAmount(amount);
        payment.setPaymentMode(paymentMode != null ? paymentMode.toUpperCase() : "CASH");
        payment.setPaymentReference(generatePaymentReference());
        payment.setPaymentDate(paymentDate != null ? paymentDate : LocalDate.now());
        payment.setStatus("COMPLETED");
        payment.setRemarks(remarks);

        TrainerPayment saved = trainerPaymentRepository.save(payment);
        return toTrainerPaymentDTO(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update payment status
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public TrainerPaymentDTO updatePaymentStatus(Long paymentId, String status) {
        TrainerPayment payment = trainerPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (!status.matches("PENDING|COMPLETED|CANCELLED")) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        payment.setStatus(status);
        TrainerPayment updated = trainerPaymentRepository.save(payment);
        return toTrainerPaymentDTO(updated);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Query methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Get all payments for a trainer
     */
    public List<TrainerPaymentDTO> getPaymentsForTrainer(Long trainerId) {
        return trainerPaymentRepository.findByTrainerId(trainerId).stream()
                .map(this::toTrainerPaymentDTO)
                .toList();
    }

    /**
     * Get all payments (admin view)
     */
    public List<TrainerPaymentDTO> getAllPayments() {
        return trainerPaymentRepository.findAll().stream()
                .map(this::toTrainerPaymentDTO)
                .toList();
    }

    /**
     * Get summary for a specific trainer
     */
    public TrainerPaymentSummaryDTO getTrainerPaymentSummary(Long trainerId) {
        TrainerProfile trainer = trainerProfileRepository.findById(trainerId)
                .orElseThrow(() -> new RuntimeException("Trainer not found: " + trainerId));

        Double totalPaid = trainerPaymentRepository.sumTotalPaidToTrainer(trainerId);
        List<TrainerPayment> completed = trainerPaymentRepository.findCompletedPaymentsForTrainer(trainerId);
        List<TrainerPayment> pending = trainerPaymentRepository.findByTrainerIdAndPaymentDateBetween(
                trainerId, LocalDate.now(), LocalDate.now().plusYears(1));
        pending = pending.stream().filter(p -> "PENDING".equals(p.getStatus())).toList();

        Double pendingAmount = pending.stream()
                .mapToDouble(TrainerPayment::getAmount)
                .sum();

        String trainerName = trainer.getUser() != null ? trainer.getUser().getName() : "Unknown";

        return new TrainerPaymentSummaryDTO(
                trainerId,
                trainerName,
                totalPaid != null ? totalPaid : 0.0,
                (long) completed.size(),
                (long) pending.size(),
                pendingAmount
        );
    }

    /**
     * Get profit/loss report
     */
    public ProfitLossReportDTO getProfitLossReport(com.example.sachitech.repository.FeeRecordRepository feeRecordRepository,
                                                    com.example.sachitech.repository.StudentProfileRepository studentProfileRepository,
                                                    com.example.sachitech.repository.FeeTransactionRepository feeTransactionRepository) {
        Double totalStudentFeesCollected = feeRecordRepository.sumTotalCollected();
        Double totalTrainerPayments = trainerPaymentRepository.sumTotalPaidToAllTrainers();
        Double profitLoss = totalStudentFeesCollected - totalTrainerPayments;
        Double profitMargin = totalStudentFeesCollected > 0 ? (profitLoss / totalStudentFeesCollected) * 100 : 0.0;

        Long totalStudents = studentProfileRepository.count();
        Long totalTrainers = trainerProfileRepository.count();
        Long totalTransactions = feeTransactionRepository.countAllTransactions();

        return new ProfitLossReportDTO(
                totalStudentFeesCollected != null ? totalStudentFeesCollected : 0.0,
                totalTrainerPayments != null ? totalTrainerPayments : 0.0,
                profitLoss != null ? profitLoss : 0.0,
                profitMargin,
                totalStudents,
                totalTrainers,
                totalTransactions
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String generatePaymentReference() {
        return "TRP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private TrainerPaymentDTO toTrainerPaymentDTO(TrainerPayment payment) {
        String trainerName = payment.getTrainer() != null && payment.getTrainer().getUser() != null
                ? payment.getTrainer().getUser().getName() : "Unknown";

        return new TrainerPaymentDTO(
                payment.getId(),
                payment.getTrainer() != null ? payment.getTrainer().getId() : null,
                trainerName,
                payment.getAmount(),
                payment.getPaymentMode(),
                payment.getPaymentReference(),
                payment.getPaymentDate(),
                payment.getStatus(),
                payment.getRemarks()
        );
    }
}
