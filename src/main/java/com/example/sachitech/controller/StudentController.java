package com.example.sachitech.controller;

import com.example.sachitech.entity.*;
import com.example.sachitech.repository.*;
import com.example.sachitech.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired private NoteRepository         noteRepository;
    @Autowired private AssignmentRepository   assignmentRepository;
    @Autowired private FeeManagementRepository feeManagementRepository;
    @Autowired private AttendanceRepository   attendanceRepository;
    @Autowired private UserRepository         userRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;
    @Autowired private JwtService             jwtService;

    // ── Ownership guard ───────────────────────────────────────────────────────
    private boolean isOwnerOrAdmin(Long studentId, HttpServletRequest req) {
        try {
            String header = req.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) return false;
            String email = jwtService.extractEmail(header.substring(7));
            var user = userRepository.findByEmail(email).orElse(null);
            if (user == null) return false;
            String role = user.getRole().name();
            if ("ADMIN".equals(role) || "MANAGER".equals(role) || "TRAINER".equals(role)) return true;
            var profile = studentProfileRepository.findByUserId(user.getId()).orElse(null);
            return profile != null && profile.getId().equals(studentId);
        } catch (Exception e) { return false; }
    }

    // ── Notes ─────────────────────────────────────────────────────────────────
    @GetMapping("/{studentId}/notes/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> viewNotes(
            @PathVariable Long studentId,
            @PathVariable Long courseId,
            HttpServletRequest req) {
        if (!isOwnerOrAdmin(studentId, req))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        return ResponseEntity.ok(noteRepository.findByCourseId(courseId));
    }

    // ── Assignments ───────────────────────────────────────────────────────────
    @GetMapping("/{studentId}/assignments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkAssignments(
            @PathVariable Long studentId,
            HttpServletRequest req) {
        if (!isOwnerOrAdmin(studentId, req))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        return ResponseEntity.ok(assignmentRepository.findByStudentId(studentId));
    }

    // ── Submission upload ─────────────────────────────────────────────────────
    @PutMapping("/upload-submission/{assignmentId}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','TRAINER')")
    public ResponseEntity<?> uploadSubmission(
            @PathVariable Long assignmentId,
            @RequestParam String link,
            HttpServletRequest req) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null)
            return ResponseEntity.notFound().build();

        // Students can only submit their own assignments
        Long ownerId = assignment.getStudent() != null ? assignment.getStudent().getId() : null;
        if (ownerId != null && !isOwnerOrAdmin(ownerId, req))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));

        assignment.setStudentSubmissionLink(link);
        assignment.setEvaluationStatus("SUBMITTED");
        return ResponseEntity.ok(assignmentRepository.save(assignment));
    }

    // ── Fee status ────────────────────────────────────────────────────────────
    @GetMapping("/{studentId}/fee-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> viewFeeStatus(
            @PathVariable Long studentId,
            HttpServletRequest req) {
        if (!isOwnerOrAdmin(studentId, req))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        return ResponseEntity.ok(feeManagementRepository.findByStudentId(studentId));
    }

    // ── Attendance status ─────────────────────────────────────────────────────
    @GetMapping("/{studentId}/attendance-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> viewAttendanceStatus(
            @PathVariable Long studentId,
            HttpServletRequest req) {
        if (!isOwnerOrAdmin(studentId, req))
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        return ResponseEntity.ok(attendanceRepository.findByStudentId(studentId));
    }
}
