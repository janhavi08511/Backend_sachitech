package com.example.sachitech.controller;

import com.example.sachitech.dto.*;
import com.example.sachitech.entity.*;
import com.example.sachitech.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private UserRepository userRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;
    @Autowired private TrainerProfileRepository trainerProfileRepository;
    @Autowired private FeeManagementRepository feeManagementRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private InternshipRepository internshipRepository;
    @Autowired private BatchRepository batchRepository;
    @Autowired private FeeRecordRepository feeRecordRepository;
    @Autowired private FeeTransactionRepository feeTransactionRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private LmsSubmissionRepository lmsSubmissionRepository;
    @Autowired private LmsContentRepository lmsContentRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    // ========================= USER =========================

    @CacheEvict(value = "users", allEntries = true)
    @PostMapping("/user/create-full")
    @Transactional
public ResponseEntity<?> createFullUser(@RequestBody Map<String, Object> payload) {
    try {

        // ================= COMMON USER =================
        String roleStr = payload.get("role").toString();
        Role role = Role.valueOf(roleStr.toUpperCase());

        User user = new User();
        user.setName((String) payload.get("name"));
        user.setEmail((String) payload.get("email"));
        user.setPassword(passwordEncoder.encode((String) payload.get("password")));
        user.setRole(role);

        userRepository.save(user);

        // ================= STUDENT =================
        if (role == Role.STUDENT) {

            Double initialPayment = payload.get("initialPayment") != null
                    ? Double.valueOf(payload.get("initialPayment").toString())
                    : 0.0;

            StudentProfile profile = new StudentProfile();
            profile.setUser(user);
            profile.setPhone((String) payload.get("phone"));
            profile.setAdmissionDate(
                payload.get("admissionDate") != null
                        ? LocalDate.parse(payload.get("admissionDate").toString())
                        : LocalDate.now()
            );
            profile.setFeePaid(initialPayment > 0);

            // Build course name string from selected courses
            List<?> rawCourses = (List<?>) payload.get("courseIds");
            List<Course> resolvedCourses = new java.util.ArrayList<>();
            if (rawCourses != null) {
                for (Object obj : rawCourses) {
                    Long courseId = Long.valueOf(obj.toString());
                    courseRepository.findById(courseId).ifPresent(resolvedCourses::add);
                }
            }

            // Set the course name on the profile (comma-joined)
            if (!resolvedCourses.isEmpty()) {
                String courseNames = resolvedCourses.stream()
                        .map(Course::getName)
                        .collect(Collectors.joining(", "));
                profile.setCourse(courseNames);
            }

            studentProfileRepository.save(profile);

            // Create FeeRecord + initial transaction for each enrolled course
            for (Course course : resolvedCourses) {
                double totalFee = course.getTotalFee() != null ? course.getTotalFee() : 0.0;

                FeeRecord record = new FeeRecord();
                record.setStudent(profile);
                record.setCourse(course);
                record.setTotalFeeAtEnrollment(totalFee);
                record.setAmountPaid(initialPayment);
                record.setPendingAmount(totalFee - initialPayment);
                record.setLastTransactionDate(LocalDate.now());

                feeRecordRepository.save(record);

                // Initial payment transaction
                if (initialPayment > 0) {
                    FeeTransaction txn = new FeeTransaction();
                    txn.setFeeRecord(record);
                    txn.setAmount(initialPayment);
                    txn.setInstallmentAmount(initialPayment);
                    txn.setPaymentDate(LocalDate.now());
                    txn.setPaymentMode("CASH");
                    txn.setTransactionType("CASH");
                    txn.setReceiptNo("INIT-" + profile.getId() + "-" + course.getId());

                    feeTransactionRepository.save(txn);
                }
            }
        }

        // ================= TRAINER =================
        if (role == Role.TRAINER) {

            TrainerProfile trainer = new TrainerProfile();
            trainer.setUser(user);
            trainer.setPhone((String) payload.get("phone"));
            trainer.setSpecialization((String) payload.get("specialization"));

            trainerProfileRepository.save(trainer);

            // Assign courses
            List<?> rawCourses = (List<?>) payload.get("courseIds");
            if (rawCourses != null) {
                List<Course> courses = rawCourses.stream()
                        .map(o -> courseRepository.findById(Long.valueOf(o.toString())).orElse(null))
                        .filter(Objects::nonNull)
                        .toList();

                trainer.setCourses(courses);
                trainerProfileRepository.save(trainer);
            }

            // Note: Salary is stored as metadata only — not as a FeeTransaction
            // (FeeTransactions are student-fee-specific and require a FeeRecord)
        }

        return ResponseEntity.ok(Map.of("message", role + " created successfully"));

    } catch (Exception e) {
        throw new RuntimeException("Creation failed", e);
    }
}
    @Cacheable("users")
    @GetMapping("/users")
    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll(Sort.by("id").descending())
                .stream()
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("name", user.getName());
                    map.put("email", user.getEmail());
                    map.put("role", user.getRole());

                    StudentProfile sp = user.getStudentProfile();
                    if (sp != null) {
                        map.put("phone", sp.getPhone());
                        map.put("course", sp.getCourse());
                    }

                    TrainerProfile tp = user.getTrainerProfile();
                    if (tp != null) {
                        map.put("phone", tp.getPhone());
                        map.put("specialization", tp.getSpecialization());
                    }

                    return map;
                })
                .toList();
    }

    @CacheEvict(value = "users", allEntries = true)
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            List<Batch> trainerBatches = batchRepository.findByTrainerId(id);
            batchRepository.deleteAll(trainerBatches);

            List<LmsContent> uploadedContent = lmsContentRepository.findByUploadedById(id);
            for (LmsContent content : uploadedContent) {
                content.setUploadedBy(null);
                lmsContentRepository.save(content);
            }

            studentProfileRepository.findByUserId(id).ifPresent(sp -> {
                Long spId = sp.getId();

                lmsSubmissionRepository.deleteAll(lmsSubmissionRepository.findByStudentId(spId));
                attendanceRepository.deleteAll(attendanceRepository.findByStudentId(spId));
                assignmentRepository.deleteAll(assignmentRepository.findByStudentId(spId));

                List<FeeRecord> feeRecords = feeRecordRepository.findByStudentId(spId);
                for (FeeRecord fr : feeRecords) {
                    feeTransactionRepository.deleteAll(
                            feeTransactionRepository.findByFeeRecordId(fr.getId()));
                }
                feeRecordRepository.deleteAll(feeRecords);

                studentProfileRepository.delete(sp);
            });

            trainerProfileRepository.findByUserId(id)
                    .ifPresent(trainerProfileRepository::delete);

            userRepository.deleteById(id);

            return ResponseEntity.ok(Map.of("message", "User deleted"));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id).orElseThrow();
        user.setPassword(passwordEncoder.encode(body.get("newPassword")));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    // ========================= STUDENT =========================

    @PostMapping("/create-student-profile")
    public ResponseEntity<?> createStudentProfile(@RequestBody StudentProfile profile) {
        try {
            // Validate user reference
            if (profile.getUser() == null || profile.getUser().getId() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User ID is required in the request body"));
            }

            // Check if user exists
            User user = userRepository.findById(profile.getUser().getId())
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User not found with id: " + profile.getUser().getId()));
            }

            // Check if user already has a student profile
            if (user.getStudentProfile() != null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User already has a student profile"));
            }

            // Validate user role
            if (user.getRole() != Role.STUDENT) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User must have STUDENT role to create a student profile"));
            }

            profile.setUser(user);
            user.setStudentProfile(profile);

            StudentProfile savedProfile = studentProfileRepository.save(profile);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedProfile.getId());
            response.put("userId", user.getId());
            response.put("userName", user.getName());
            response.put("userEmail", user.getEmail());
            response.put("phone", savedProfile.getPhone());
            response.put("course", savedProfile.getCourse());
            response.put("admissionDate", savedProfile.getAdmissionDate());
            response.put("feePaid", savedProfile.getFeePaid());
            response.put("message", "Student profile created successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to create student profile: " + e.getMessage()));
        }
    }

    @GetMapping("/studentdata/all")
    public List<Map<String, Object>> getAllStudentProfiles() {

        return studentProfileRepository.findAll(Sort.by("id").descending())
                .stream().map(profile -> {

                    Map<String, Object> map = new HashMap<>();

                    map.put("id", profile.getId());
                    map.put("name", profile.getUser() != null ? profile.getUser().getName() : "Unknown");
                    map.put("email", profile.getUser() != null ? profile.getUser().getEmail() : "N/A");
                    map.put("phone", profile.getPhone() != null ? profile.getPhone() : "N/A");
                    map.put("course", profile.getCourse() != null ? profile.getCourse() : "N/A");
                    map.put("admissiondate",
                            profile.getAdmissionDate() != null ? profile.getAdmissionDate().toString() : "N/A");

                    return map;
                }).toList();
    }

    // ========================= TRAINER =========================

    @PostMapping("/create-trainer-profile")
    public ResponseEntity<?> createTrainerProfile(@RequestBody TrainerProfile profile) {
        try {
            // Validate user reference
            if (profile.getUser() == null || profile.getUser().getId() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User ID is required in the request body"));
            }

            // Check if user exists
            User user = userRepository.findById(profile.getUser().getId())
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User not found with id: " + profile.getUser().getId()));
            }

            // Check if user already has a trainer profile
            if (user.getTrainerProfile() != null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User already has a trainer profile"));
            }

            // Validate user role
            if (user.getRole() != Role.TRAINER) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User must have TRAINER role to create a trainer profile"));
            }

            profile.setUser(user);
            user.setTrainerProfile(profile);

            TrainerProfile savedProfile = trainerProfileRepository.save(profile);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedProfile.getId());
            response.put("userId", user.getId());
            response.put("userName", user.getName());
            response.put("userEmail", user.getEmail());
            response.put("message", "Trainer profile created successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to create trainer profile: " + e.getMessage()));
        }
    }

    // ========================= COURSE =========================

    @PostMapping("/add-course")
    public CourseDTO addCourse(@RequestBody Course course) {
        Course saved = courseRepository.save(course);
        return convertToCourseDTO(saved);
    }

    @GetMapping("/courses")
    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll(Sort.by("id").descending())
                .stream()
                .map(this::convertToCourseDTO)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/delete-course/{id}")
    public void deleteCourse(@PathVariable Long id) {
        courseRepository.deleteById(id);
    }

    // ========================= BATCH =========================

    @PostMapping("/add-batch")
    public BatchDTO addBatch(@RequestParam(required = false) Long courseId,
                          @RequestParam(required = false) Long internshipId,
                          @RequestParam(required = false) Long trainerId,
                          @RequestBody Batch batch) {

        if (courseId != null)
            batch.setCourse(courseRepository.findById(courseId).orElse(null));

        if (internshipId != null)
            batch.setInternship(internshipRepository.findById(internshipId).orElse(null));

        if (trainerId != null)
            batch.setTrainer(userRepository.findById(trainerId).orElse(null));

        Batch saved = batchRepository.save(batch);
        return convertToBatchDTO(saved);
    }

    @GetMapping("/batches")
    public List<BatchDTO> getAllBatches() {
        return batchRepository.findAll(Sort.by("id").descending())
                .stream()
                .map(this::convertToBatchDTO)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/delete-batch/{id}")
    public ResponseEntity<?> deleteBatch(@PathVariable Long id) {
        try {
            if (!batchRepository.existsById(id)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Batch not found with id: " + id));
            }
            batchRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Batch deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to delete batch: " + e.getMessage()));
        }
    }

    // ========================= INTERNSHIP =========================

    @PostMapping("/add-internship")
    public InternshipDTO addInternship(@RequestBody Internship internship) {
        Internship saved = internshipRepository.save(internship);
        return convertToInternshipDTO(saved);
    }

    @GetMapping("/internships")
    public List<InternshipDTO> getAllInternships() {
        return internshipRepository.findAll(Sort.by("id").descending())
                .stream()
                .map(this::convertToInternshipDTO)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/delete-internship/{id}")
    public ResponseEntity<Void> deleteInternship(@PathVariable Long id) {
        internshipRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ========================= FEES =========================

    @GetMapping("/fees/student-status")
    public List<Map<String, Object>> getAllStudentFeeStatusMapped() {

        return feeManagementRepository.findAll(Sort.by("id").descending())
                .stream().map(f -> {

                    Map<String, Object> map = new HashMap<>();

                    map.put("studentId", f.getStudent() != null ? f.getStudent().getId() : null);
                    map.put("totalFees", f.getFeeAmount() != null ? f.getFeeAmount() : 0.0);

                    double paid = f.getFeeAmount() != null
                            ? f.getFeeAmount() - (f.getPendingAmount() != null ? f.getPendingAmount() : 0.0)
                            : 0.0;

                    map.put("paidAmount", paid);

                    return map;

                }).toList();
    }

    // ========================= HELPER METHODS =========================

    private CourseDTO convertToCourseDTO(Course course) {
        return new CourseDTO(
                course.getId(),
                course.getName(),
                course.getDuration(),
                course.getCategory(),
                course.getDescription(),
                course.getTotalFee(),
                course.getStatus(),
                course.getPrerequisite(),
                course.getProgress()
        );
    }

    private BatchDTO convertToBatchDTO(Batch batch) {
        return new BatchDTO(
                batch.getId(),
                batch.getName(),
                batch.getStartDate(),
                batch.getEndDate(),
                batch.getStatus(),
                batch.getCourse() != null ? batch.getCourse().getId() : null,
                batch.getInternship() != null ? batch.getInternship().getId() : null,
                batch.getTrainer() != null ? batch.getTrainer().getId() : null
        );
    }

    private InternshipDTO convertToInternshipDTO(Internship internship) {
        return new InternshipDTO(
                internship.getId(),
                internship.getName(),
                internship.getDuration(),
                internship.getCategory(),
                internship.getTotalFee(),
                internship.getStatus(),
                internship.getPrerequisite(),
                internship.getProgress()
        );
    }
}
