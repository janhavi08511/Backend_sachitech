package com.example.sachitech.service;

import com.example.sachitech.entity.*;
import com.example.sachitech.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class FeeService {

    @Autowired
    private FeeRecordRepository feeRecordRepository;

    @Autowired
    private FeeTransactionRepository feeTransactionRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private CourseRepository courseRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // DTOs (inner classes — keeps things self-contained)
    // ─────────────────────────────────────────────────────────────────────────

    public record CollectInstallmentRequest(
            Long studentId,
            Long courseId,
            Double installmentAmount,
            String transactionType,   // CASH | ONLINE | CHEQUE
            String receiptNo,
            LocalDate paymentDate
    ) {}

    public record FeeRecordDTO(
            Long feeRecordId,
            Long studentId,
            String studentName,
            String studentEmail,
            Long courseId,
            String courseName,
            Double totalFeeAtEnrollment,
            Double amountPaid,
            Double pendingAmount,
            LocalDate lastTransactionDate
    ) {}

    public record TransactionDTO(
            Long transactionId,
            Long feeRecordId,
            Long studentId,
            String studentName,
            Long courseId,
            String courseName,
            Double installmentAmount,
            LocalDate paymentDate,
            String transactionType,
            String receiptNo
    ) {}

    public record FeeStatsDTO(
            Double totalExpectedRevenue,
            Double totalCollected,
            Double totalPending,
            Double thisMonthCollected,
            Long totalTransactions
    ) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Enrollment: create a FeeRecord when a student enrolls in a course
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public FeeRecord createFeeRecord(Long studentId, Long courseId) {
        StudentProfile student = studentProfileRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        // Idempotent: return existing record if already enrolled
        Optional<FeeRecord> existing = feeRecordRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Double fee = course.getTotalFee() != null ? course.getTotalFee() : 0.0;

        FeeRecord record = new FeeRecord();
        record.setStudent(student);
        record.setCourse(course);
        record.setTotalFeeAtEnrollment(fee);
        record.setAmountPaid(0.0);
        record.setPendingAmount(fee);
        return feeRecordRepository.save(record);
    }

    /**
     * Record initial payment when a student is created/enrolled
     * This creates a FeeRecord and optionally records an initial payment
     */
    @Transactional
    public FeeRecord createStudentWithInitialPayment(Long studentId, Long courseId, Double initialPayment) {
        FeeRecord record = createFeeRecord(studentId, courseId);

        // If initial payment is provided, record it
        if (initialPayment != null && initialPayment > 0) {
            CollectInstallmentRequest req = new CollectInstallmentRequest(
                    studentId,
                    courseId,
                    initialPayment,
                    "CASH",
                    null,
                    LocalDate.now()
            );
            collectInstallment(req);
            // Refresh the record to get updated amounts
            record = feeRecordRepository.findById(record.getId()).orElse(record);
        }

        return record;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Collect an installment — core business logic
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public TransactionDTO collectInstallment(CollectInstallmentRequest req) {
        // 1. Find or auto-create the fee record
        FeeRecord record = feeRecordRepository
                .findByStudentIdAndCourseId(req.studentId(), req.courseId())
                .orElseGet(() -> createFeeRecord(req.studentId(), req.courseId()));

        // 2. Validate amount
        if (req.installmentAmount() == null || req.installmentAmount() <= 0) {
            throw new IllegalArgumentException("Installment amount must be positive");
        }
        if (req.installmentAmount() > record.getPendingAmount()) {
            throw new IllegalArgumentException(
                    "Installment ₹" + req.installmentAmount() +
                    " exceeds pending amount ₹" + record.getPendingAmount());
        }

        // 3. Persist the transaction
        FeeTransaction tx = new FeeTransaction();
        tx.setFeeRecord(record);
        tx.setInstallmentAmount(req.installmentAmount());
        tx.setPaymentDate(req.paymentDate() != null ? req.paymentDate() : LocalDate.now());
        tx.setTransactionType(req.transactionType() != null ? req.transactionType().toUpperCase() : "CASH");
        tx.setReceiptNo(req.receiptNo() != null ? req.receiptNo() : generateReceiptNo());
        feeTransactionRepository.save(tx);

        // 4. Auto-update the FeeRecord totals
        record.setAmountPaid(record.getAmountPaid() + req.installmentAmount());
        record.setPendingAmount(record.getTotalFeeAtEnrollment() - record.getAmountPaid());
        record.setLastTransactionDate(tx.getPaymentDate());
        feeRecordRepository.save(record);

        return toTransactionDTO(tx);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────

    /** Admin: all fee records */
    public List<FeeRecordDTO> getAllFeeRecords() {
        return feeRecordRepository.findAll().stream()
                .map(this::toFeeRecordDTO)
                .toList();
    }

    /** Student: their own fee records only */
    public List<FeeRecordDTO> getFeeRecordsForStudent(Long studentId) {
        return feeRecordRepository.findByStudentId(studentId).stream()
                .map(this::toFeeRecordDTO)
                .toList();
    }

    /** Student: their own transactions only */
    public List<TransactionDTO> getTransactionsForStudent(Long studentId) {
        return feeTransactionRepository
                .findByFeeRecordStudentIdOrderByPaymentDateDesc(studentId).stream()
                .map(this::toTransactionDTO)
                .toList();
    }

    /** Admin: all transactions, optionally filtered */
    public List<TransactionDTO> getAllTransactions(String studentName, LocalDate from, LocalDate to) {
        List<FeeTransaction> txList;

        if (studentName != null && !studentName.isBlank()) {
            txList = feeTransactionRepository.findByStudentNameContaining(studentName.trim());
        } else if (from != null && to != null) {
            txList = feeTransactionRepository.findByDateRange(from, to);
        } else {
            txList = feeTransactionRepository.findAll(
                    org.springframework.data.domain.Sort.by(
                            org.springframework.data.domain.Sort.Direction.DESC, "paymentDate"));
        }

        return txList.stream().map(this::toTransactionDTO).toList();
    }

    /** Admin summary stats */
    public FeeStatsDTO getStats() {
        YearMonth now = YearMonth.now();
        return new FeeStatsDTO(
                feeRecordRepository.sumTotalExpectedRevenue(),
                feeRecordRepository.sumTotalCollected(),
                feeRecordRepository.sumTotalPending(),
                feeTransactionRepository.sumCollectedInMonth(now.getYear(), now.getMonthValue()),
                feeTransactionRepository.countAllTransactions()
        );
    }

    /**
     * Get fee status for a specific student
     */
    public record StudentFeeStatusDTO(
            Long studentId,
            String studentName,
            Double totalExpected,
            Double totalPaid,
            Double pendingAmount,
            Double paymentPercentage,
            String status  // PAID, PARTIAL, PENDING
    ) {}

    public StudentFeeStatusDTO getStudentFeeStatus(Long studentId) {
        StudentProfile student = studentProfileRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

        List<FeeRecord> records = feeRecordRepository.findByStudentId(studentId);
        
        Double totalExpected = records.stream()
                .mapToDouble(FeeRecord::getTotalFeeAtEnrollment)
                .sum();
        Double totalPaid = records.stream()
                .mapToDouble(FeeRecord::getAmountPaid)
                .sum();
        Double pendingAmount = records.stream()
                .mapToDouble(FeeRecord::getPendingAmount)
                .sum();

        Double paymentPercentage = totalExpected > 0 ? (totalPaid / totalExpected) * 100 : 0.0;
        
        String status = "PENDING";
        if (pendingAmount <= 0) {
            status = "PAID";
        } else if (totalPaid > 0) {
            status = "PARTIAL";
        }

        String studentName = student.getUser() != null ? student.getUser().getName() : "Unknown";

        return new StudentFeeStatusDTO(
                studentId,
                studentName,
                totalExpected,
                totalPaid,
                pendingAmount,
                paymentPercentage,
                status
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String generateReceiptNo() {
        return "RCP-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private FeeRecordDTO toFeeRecordDTO(FeeRecord r) {
        String studentName = (r.getStudent() != null && r.getStudent().getUser() != null)
                ? r.getStudent().getUser().getName() : "Unknown";
        String studentEmail = (r.getStudent() != null && r.getStudent().getUser() != null)
                ? r.getStudent().getUser().getEmail() : "";
        String courseName = r.getCourse() != null ? r.getCourse().getName() : "Unknown";
        Long courseId = r.getCourse() != null ? r.getCourse().getId() : null;

        return new FeeRecordDTO(
                r.getId(),
                r.getStudent() != null ? r.getStudent().getId() : null,
                studentName,
                studentEmail,
                courseId,
                courseName,
                r.getTotalFeeAtEnrollment(),
                r.getAmountPaid(),
                r.getPendingAmount(),
                r.getLastTransactionDate()
        );
    }

    private TransactionDTO toTransactionDTO(FeeTransaction t) {
        FeeRecord rec = t.getFeeRecord();
        String studentName = (rec.getStudent() != null && rec.getStudent().getUser() != null)
                ? rec.getStudent().getUser().getName() : "Unknown";
        String courseName = rec.getCourse() != null ? rec.getCourse().getName() : "Unknown";

        return new TransactionDTO(
                t.getId(),
                rec.getId(),
                rec.getStudent() != null ? rec.getStudent().getId() : null,
                studentName,
                rec.getCourse() != null ? rec.getCourse().getId() : null,
                courseName,
                t.getInstallmentAmount(),
                t.getPaymentDate(),
                t.getTransactionType(),
                t.getReceiptNo()
        );
    }
}
