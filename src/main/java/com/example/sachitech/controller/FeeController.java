package com.example.sachitech.controller;

import com.example.sachitech.entity.FeeRecord;
import com.example.sachitech.repository.StudentProfileRepository;
import com.example.sachitech.repository.UserRepository;
import com.example.sachitech.security.JwtService;
import com.example.sachitech.service.FeeService;
import com.example.sachitech.service.FeeService.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Fee Management REST Controller
 *
 * Admin endpoints  → /api/fees/...  (ADMIN / MANAGER only)
 * Student endpoint → /api/fees/student/{id}  (STUDENT — own data only)
 */
@RestController
@RequestMapping("/api/fees")
public class FeeController {

    @Autowired
    private FeeService feeService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN: Collect a new installment
    // POST /api/fees/collect
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/collect")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TransactionDTO> collectInstallment(
            @RequestBody CollectInstallmentRequest request) {
        TransactionDTO result = feeService.collectInstallment(request);
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN: Create a fee record on enrollment
    // POST /api/fees/enroll?studentId=&courseId=
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/enroll")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<FeeRecord> enrollStudentInCourse(
            @RequestParam Long studentId,
            @RequestParam Long courseId) {
        FeeRecord record = feeService.createFeeRecord(studentId, courseId);
        return ResponseEntity.ok(record);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN: All fee records (summary per student-course)
    // GET /api/fees/records
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<FeeRecordDTO>> getAllFeeRecords() {
        return ResponseEntity.ok(feeService.getAllFeeRecords());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN: All transactions (master list), searchable
    // GET /api/fees/transactions?studentName=&from=&to=
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<TransactionDTO>> getAllTransactions(
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(feeService.getAllTransactions(studentName, from, to));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN: Summary stats dashboard
    // GET /api/fees/stats
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<FeeStatsDTO> getStats() {
        return ResponseEntity.ok(feeService.getStats());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STUDENT: Own fee records (summary cards)
    // GET /api/fees/student/{id}
    // Security: students can only access their OWN data
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STUDENT')")
    public ResponseEntity<?> getStudentFeeRecords(
            @PathVariable Long studentId,
            HttpServletRequest request) {

        // Security check: students can only view their own data
        if (!isAdminOrOwner(studentId, request)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied: you can only view your own fee data"));
        }

        return ResponseEntity.ok(feeService.getFeeRecordsForStudent(studentId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STUDENT: Own transaction history
    // GET /api/fees/student/{id}/transactions
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/student/{studentId}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STUDENT')")
    public ResponseEntity<?> getStudentTransactions(
            @PathVariable Long studentId,
            HttpServletRequest request) {

        if (!isAdminOrOwner(studentId, request)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Access denied: you can only view your own transactions"));
        }

        return ResponseEntity.ok(feeService.getTransactionsForStudent(studentId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: verify the caller is ADMIN/MANAGER, or is the student themselves
    // ─────────────────────────────────────────────────────────────────────────
    private boolean isAdminOrOwner(Long studentId, HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) return false;

            String token = authHeader.substring(7);
            String email = jwtService.extractEmail(token);

            var user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return false;

            // Admins and managers can access any student's data
            String role = user.getRole().name();
            if ("ADMIN".equals(role) || "MANAGER".equals(role)) return true;

            // Students: verify the studentId belongs to their profile
            var profile = studentProfileRepository.findByUserId(user.getId()).orElse(null);
            return profile != null && profile.getId().equals(studentId);

        } catch (Exception e) {
            return false;
        }
    }
}
