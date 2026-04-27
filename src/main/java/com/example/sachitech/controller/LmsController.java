package com.example.sachitech.controller;

import com.example.sachitech.entity.LmsContent;
import com.example.sachitech.entity.LmsSubmission;
import com.example.sachitech.service.LmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lms")
@RequiredArgsConstructor
public class LmsController {

    private final LmsService lmsService;

    // ─── DTO HELPERS ──────────────────────────────────────────────────────────

    private Map<String, Object> toContentDTO(LmsContent c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("title", c.getTitle());
        map.put("type", c.getType() != null ? c.getType().name() : null);
        map.put("fileUrl", c.getFileUrl());
        map.put("uploadDate", c.getUploadDate() != null ? c.getUploadDate().toString() : null);

        if (c.getCourse() != null) {
            map.put("courseId", c.getCourse().getId());
            map.put("courseName", c.getCourse().getName());
        }
        if (c.getInternship() != null) {
            map.put("internshipId", c.getInternship().getId());
            map.put("internshipName", c.getInternship().getName());
        }
        if (c.getUploadedBy() != null) {
            map.put("uploadedById", c.getUploadedBy().getId());
            map.put("uploadedByName", c.getUploadedBy().getName());
        }
        return map;
    }

    private Map<String, Object> toSubmissionDTO(LmsSubmission s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("fileUrl", s.getFileUrl());
        map.put("submissionDate", s.getSubmissionDate() != null ? s.getSubmissionDate().toString() : null);
        map.put("status", s.getStatus() != null ? s.getStatus().name() : null);
        map.put("score", s.getScore());
        map.put("feedback", s.getFeedback());

        if (s.getAssignment() != null) {
            map.put("assignmentId", s.getAssignment().getId());
            map.put("assignmentTitle", s.getAssignment().getTitle());
            map.put("assignmentType", s.getAssignment().getType() != null ? s.getAssignment().getType().name() : null);
            if (s.getAssignment().getCourse() != null) {
                map.put("courseName", s.getAssignment().getCourse().getName());
            }
        }
        if (s.getStudent() != null) {
            map.put("studentId", s.getStudent().getId());
            if (s.getStudent().getUser() != null) {
                map.put("studentName", s.getStudent().getUser().getName());
                map.put("studentEmail", s.getStudent().getUser().getEmail());
            }
        }
        return map;
    }

    // ─── UPLOAD ───────────────────────────────────────────────────────────────

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam MultipartFile file,
            @RequestParam String title,
            @RequestParam String type,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long internshipId,
            Authentication auth) {

        LmsContent saved = lmsService.uploadContent(
                file,
                title,
                LmsContent.ContentType.valueOf(type.toUpperCase()),
                courseId,
                internshipId,
                auth.getName()
        );

        return ResponseEntity.ok(toContentDTO(saved));
    }

    // ─── CONTENT BY COURSE ────────────────────────────────────────────────────

    @GetMapping("/content/{courseId}")
    public ResponseEntity<List<Map<String, Object>>> getContent(@PathVariable Long courseId) {
        List<Map<String, Object>> result = lmsService.getContentByCourse(courseId)
                .stream()
                .map(this::toContentDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ─── CONTENT BY INTERNSHIP ────────────────────────────────────────────────

    @GetMapping("/internship-content/{internshipId}")
    public ResponseEntity<List<Map<String, Object>>> getInternshipContent(@PathVariable Long internshipId) {
        List<Map<String, Object>> result = lmsService.getContentByInternship(internshipId)
                .stream()
                .map(this::toContentDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ─── STUDENT: SUBMIT ──────────────────────────────────────────────────────

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> submit(
            @RequestParam MultipartFile file,
            @RequestParam Long assignmentId,
            @RequestParam Long studentId) {

        LmsSubmission saved = lmsService.submitAssignment(file, assignmentId, studentId);
        return ResponseEntity.ok(toSubmissionDTO(saved));
    }

    // ─── ALL SUBMISSIONS ──────────────────────────────────────────────────────

    @GetMapping("/submissions")
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        List<Map<String, Object>> result = lmsService.getAllSubmissions()
                .stream()
                .map(this::toSubmissionDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ─── EVALUATE ─────────────────────────────────────────────────────────────

    @PutMapping("/evaluate/{id}")
    public ResponseEntity<Map<String, Object>> eval(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        LmsSubmission evaluated = lmsService.evaluateSubmission(
                id,
                Double.parseDouble(body.get("score").toString()),
                (String) body.get("feedback")
        );

        return ResponseEntity.ok(toSubmissionDTO(evaluated));
    }
}