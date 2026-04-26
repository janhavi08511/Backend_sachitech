package com.example.sachitech.controller;

import com.example.sachitech.entity.*;
import com.example.sachitech.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/trainer")
public class TrainerController {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrainerProfileRepository trainerProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private InternshipRepository internshipRepository;

    @Autowired
    private BatchRepository batchRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/trainer/dashboard
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('TRAINER')")
    public ResponseEntity<Map<String, Object>> getTrainerDashboard(Authentication auth) {
        try {
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            TrainerProfile trainer = trainerProfileRepository.findByUserId(user.getId())
                    .orElse(null);

            Map<String, Object> dashboard = new HashMap<>();

            if (trainer == null) {
                // Trainer profile not found, return zeros
                dashboard.put("totalStudents", 0);
                dashboard.put("totalCourses", 0);
                dashboard.put("totalInternships", 0);
                return ResponseEntity.ok(dashboard);
            }

            // Count courses taught
            int totalCourses = trainer.getCoursesTaught() != null ? trainer.getCoursesTaught().size() : 0;

            // Count internships taught
            int totalInternships = trainer.getInternshipsTaught() != null ? trainer.getInternshipsTaught().size() : 0;

            // Count total students (enrolled in trainer's courses/internships)
            Set<Long> studentIds = new HashSet<>();
            if (trainer.getCoursesTaught() != null && !trainer.getCoursesTaught().isEmpty()) {
                for (Course course : trainer.getCoursesTaught()) {
                    List<StudentProfile> enrolled = studentProfileRepository.findAll().stream()
                            .filter(sp -> sp.getEnrolledCourses() != null
                                    && sp.getEnrolledCourses().stream().anyMatch(c -> c.getId().equals(course.getId())))
                            .toList();
                    enrolled.forEach(sp -> studentIds.add(sp.getId()));
                }
            }
            if (trainer.getInternshipsTaught() != null && !trainer.getInternshipsTaught().isEmpty()) {
                for (Internship internship : trainer.getInternshipsTaught()) {
                    List<StudentProfile> enrolled = studentProfileRepository.findAll().stream()
                            .filter(sp -> sp.getEnrolledInternships() != null
                                    && sp.getEnrolledInternships().stream().anyMatch(i -> i.getId().equals(internship.getId())))
                            .toList();
                    enrolled.forEach(sp -> studentIds.add(sp.getId()));
                }
            }

            dashboard.put("totalStudents", studentIds.size());
            dashboard.put("totalCourses", totalCourses);
            dashboard.put("totalInternships", totalInternships);

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/trainer/batches — Trainer's batches only
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/batches")
    @PreAuthorize("hasRole('TRAINER')")
    public ResponseEntity<List<Batch>> getTrainerBatches(Authentication auth) {
        try {
            User user = userRepository.findByEmail(auth.getName())
                    .orElse(null);
            
            if (user == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<Batch> allBatches = batchRepository.findAll();
            List<Batch> trainerBatches = allBatches.stream()
                    .filter(b -> b.getTrainer() != null && b.getTrainer().getId().equals(user.getId()))
                    .toList();

            return ResponseEntity.ok(trainerBatches);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/batches/course/{courseId}")
    @PreAuthorize("hasRole('TRAINER')")
    public ResponseEntity<List<Batch>> getTrainerBatchesByCourse(
            @PathVariable Long courseId,
            Authentication auth) {
        try {
            User user = userRepository.findByEmail(auth.getName())
                    .orElse(null);
            
            if (user == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<Batch> batches = batchRepository.findByTrainerIdAndCourseId(user.getId(), courseId);
            return ResponseEntity.ok(batches);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/batches/internship/{internshipId}")
    @PreAuthorize("hasRole('TRAINER')")
    public ResponseEntity<List<Batch>> getTrainerBatchesByInternship(
            @PathVariable Long internshipId,
            Authentication auth) {
        try {
            User user = userRepository.findByEmail(auth.getName())
                    .orElse(null);
            
            if (user == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<Batch> batches = batchRepository.findByTrainerIdAndInternshipId(user.getId(), internshipId);
            return ResponseEntity.ok(batches);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/trainer/students — Students in trainer's courses/internships
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/students")
    @PreAuthorize("hasRole('TRAINER')")
    public ResponseEntity<List<Map<String, Object>>> getTrainerStudents(Authentication auth) {
        try {
            User user = userRepository.findByEmail(auth.getName())
                    .orElse(null);
            
            if (user == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            TrainerProfile trainer = trainerProfileRepository.findByUserId(user.getId())
                    .orElse(null);
            
            if (trainer == null) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            Set<StudentProfile> students = new HashSet<>();

            // Get students enrolled in trainer's courses
            if (trainer.getCoursesTaught() != null && !trainer.getCoursesTaught().isEmpty()) {
                for (Course course : trainer.getCoursesTaught()) {
                    studentProfileRepository.findAll().stream()
                            .filter(sp -> sp.getEnrolledCourses() != null
                                    && sp.getEnrolledCourses().stream().anyMatch(c -> c.getId().equals(course.getId())))
                            .forEach(students::add);
                }
            }

            // Get students enrolled in trainer's internships
            if (trainer.getInternshipsTaught() != null && !trainer.getInternshipsTaught().isEmpty()) {
                for (Internship internship : trainer.getInternshipsTaught()) {
                    studentProfileRepository.findAll().stream()
                            .filter(sp -> sp.getEnrolledInternships() != null
                                    && sp.getEnrolledInternships().stream().anyMatch(i -> i.getId().equals(internship.getId())))
                            .forEach(students::add);
                }
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (StudentProfile sp : students) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", sp.getId());
                if (sp.getUser() != null) {
                    map.put("name", sp.getUser().getName());
                    map.put("email", sp.getUser().getEmail());
                } else {
                    map.put("name", "Unknown");
                    map.put("email", "");
                }
                map.put("phone", sp.getPhone() != null ? sp.getPhone() : "N/A");
                result.add(map);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Legacy endpoints — secured with @PreAuthorize
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/upload-notes")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Note uploadNotes(@org.springframework.lang.NonNull @RequestBody Note note) {
        return noteRepository.save(note);
    }

    @PostMapping("/create-assignment")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Assignment createAssignment(@org.springframework.lang.NonNull @RequestBody Assignment assignment) {
        return assignmentRepository.save(assignment);
    }

    @PutMapping("/grade-assignment/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Assignment gradeAssignment(@org.springframework.lang.NonNull @PathVariable Long id, @RequestBody Assignment updatedAssignment) {
        Assignment assignment = assignmentRepository.findById(id).orElseThrow();
        assignment.setScore(updatedAssignment.getScore());
        assignment.setEvaluationStatus(updatedAssignment.getEvaluationStatus());
        return assignmentRepository.save(assignment);
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    @PostMapping("/mark-attendance")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Attendance markAttendance(@org.springframework.lang.NonNull @RequestBody Attendance attendance) {
        return attendanceRepository.save(attendance);
    }
}
