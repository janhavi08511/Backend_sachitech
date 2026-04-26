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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lms")
@RequiredArgsConstructor
public class LmsController {

    private final LmsService lmsService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<LmsContent> upload(
            @RequestParam MultipartFile file,
            @RequestParam String title,
            @RequestParam String type,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long internshipId,
            Authentication auth) {

        return ResponseEntity.ok(
                lmsService.uploadContent(
                        file,
                        title,
                        LmsContent.ContentType.valueOf(type.toUpperCase()),
                        courseId,
                        internshipId,
                        auth.getName()
                )
        );
    }

    @GetMapping("/content/{courseId}")
    public ResponseEntity<List<LmsContent>> getContent(@PathVariable Long courseId) {
        return ResponseEntity.ok(lmsService.getContentByCourse(courseId));
    }

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LmsSubmission> submit(
            @RequestParam MultipartFile file,
            @RequestParam Long assignmentId,
            @RequestParam Long studentId) {

        return ResponseEntity.ok(
                lmsService.submitAssignment(file, assignmentId, studentId)
        );
    }

    @GetMapping("/submissions")
    public ResponseEntity<List<LmsSubmission>> getAll() {
        return ResponseEntity.ok(lmsService.getAllSubmissions());
    }

    @PutMapping("/evaluate/{id}")
    public ResponseEntity<LmsSubmission> eval(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        return ResponseEntity.ok(
                lmsService.evaluateSubmission(
                        id,
                        Double.parseDouble(body.get("score").toString()),
                        (String) body.get("feedback")
                )
        );
    }
}