package com.example.sachitech.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.sachitech.entity.*;
import com.example.sachitech.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LmsService {

    private final LmsContentRepository contentRepository;
    private final LmsSubmissionRepository submissionRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final InternshipRepository internshipRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final Cloudinary cloudinary;

    // ✅ FIXED Cloudinary Upload
    public String uploadToCloudinary(MultipartFile file) {
        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "auto",   // ✅ supports pdf + images
                            "type", "upload",          // ✅ PUBLIC access (fixes 401)
                            "folder", "lms_files"      // ✅ organized storage
                    )
            );

            return uploadResult.get("secure_url").toString();

        } catch (Exception e) {
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }

    // ─────────────────────────────────────────────
    // CONTENT UPLOAD
    // ─────────────────────────────────────────────

    public LmsContent uploadContent(MultipartFile file,
                                    String title,
                                    LmsContent.ContentType type,
                                    Long courseId,
                                    Long internshipId,
                                    String uploaderEmail) {

        String fileUrl = uploadToCloudinary(file);

        LmsContent content = new LmsContent();
        content.setTitle(title);
        content.setType(type);
        content.setFileUrl(fileUrl);
        content.setUploadDate(LocalDateTime.now());

        if (courseId != null) {
            content.setCourse(courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found")));
        }

        if (internshipId != null) {
            content.setInternship(internshipRepository.findById(internshipId)
                    .orElseThrow(() -> new RuntimeException("Internship not found")));
        }

        User uploader = userRepository.findByEmail(uploaderEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        content.setUploadedBy(uploader);

        return contentRepository.save(content);
    }

    // ─────────────────────────────────────────────
    // SUBMISSION
    // ─────────────────────────────────────────────

    public LmsSubmission submitAssignment(MultipartFile file,
                                          Long assignmentId,
                                          Long studentProfileId) {

        LmsContent assignment = contentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        if (assignment.getType() != LmsContent.ContentType.ASSIGNMENT) {
            throw new RuntimeException("Not an assignment");
        }

        StudentProfile student = studentProfileRepository.findById(studentProfileId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        String fileUrl = uploadToCloudinary(file);

        LmsSubmission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentProfileId)
                .orElse(new LmsSubmission());

        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setFileUrl(fileUrl);
        submission.setSubmissionDate(LocalDateTime.now());
        submission.setStatus(LmsSubmission.SubmissionStatus.SUBMITTED);

        return submissionRepository.save(submission);
    }

    // ─────────────────────────────────────────────
    // FETCH
    // ─────────────────────────────────────────────

    public List<LmsContent> getContentByCourse(Long courseId) {
        return contentRepository.findByCourseId(courseId);
    }

    public List<LmsSubmission> getAllSubmissions() {
        return submissionRepository.findAll();
    }

    public LmsSubmission evaluateSubmission(Long id, Double score, String feedback) {
        LmsSubmission s = submissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        s.setScore(score);
        s.setFeedback(feedback);
        s.setStatus(LmsSubmission.SubmissionStatus.EVALUATED);

        return submissionRepository.save(s);
    }
}
