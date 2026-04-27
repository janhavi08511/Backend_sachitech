package com.example.sachitech.controller;

import com.example.sachitech.entity.*;
import com.example.sachitech.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FeeRecordRepository feeRecordRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/attendance/course/{courseId}/students
    // Returns students enrolled in a course — uses FeeRecord as enrollment source
    // (FeeRecord is populated by the fee enrollment flow and DataSheetSeeder)
    // ─────────────────────────────────────────────────────────────────────────
    @Cacheable(value = "courseStudents", key = "#courseId")
    @GetMapping("/course/{courseId}/students")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<List<Map<String, Object>>> getStudentsForCourse(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return ResponseEntity.ok(new ArrayList<>());

        // Primary: students with a FeeRecord for this course (covers DataSheetSeeder data)
        List<FeeRecord> feeRecords = feeRecordRepository.findByCourseId(courseId);
        Set<Long> addedIds = new HashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();

        for (FeeRecord fr : feeRecords) {
            StudentProfile sp = fr.getStudent();
            if (sp == null || addedIds.contains(sp.getId())) continue;
            addedIds.add(sp.getId());
            Map<String, Object> map = new HashMap<>();
            map.put("id",    sp.getId());
            map.put("name",  sp.getUser() != null ? sp.getUser().getName()  : "Unknown");
            map.put("email", sp.getUser() != null ? sp.getUser().getEmail() : "");
            map.put("phone", sp.getPhone() != null ? sp.getPhone() : "");
            result.add(map);
        }

        // Fallback: also include students enrolled via the ManyToMany join table
        List<StudentProfile> allStudents = studentProfileRepository.findAll();
        for (StudentProfile sp : allStudents) {
            if (addedIds.contains(sp.getId())) continue;
            boolean enrolled = sp.getEnrolledCourses() != null &&
                    sp.getEnrolledCourses().stream().anyMatch(c -> c.getId().equals(courseId));
            if (!enrolled) continue;
            addedIds.add(sp.getId());
            Map<String, Object> map = new HashMap<>();
            map.put("id",    sp.getId());
            map.put("name",  sp.getUser() != null ? sp.getUser().getName()  : "Unknown");
            map.put("email", sp.getUser() != null ? sp.getUser().getEmail() : "");
            map.put("phone", sp.getPhone() != null ? sp.getPhone() : "");
            result.add(map);
        }

        result.sort(Comparator.comparing(m -> m.get("name").toString()));
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/attendance/lock/{courseId}
    // Concurrency control: lock a course for marking (409 if already locked)
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/lock/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<Map<String, Object>> lockCourse(
            @PathVariable Long courseId,
            Authentication auth) {

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return ResponseEntity.notFound().build();

        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        // If already locked by someone else → 409 Conflict
        if (course.isMarkingActive() && !Objects.equals(course.getMarkingLockedBy(), user.getId())) {
            return ResponseEntity.status(409).body(Map.of(
                "error", "Attendance marking is already in progress for this course",
                "lockedBy", course.getMarkingLockedBy()
            ));
        }

        course.setMarkingActive(true);
        course.setMarkingLockedBy(user.getId());
        courseRepository.save(course);

        return ResponseEntity.ok(Map.of("locked", true, "courseId", courseId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/attendance/unlock/{courseId}
    // Release the lock after submission
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/unlock/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<Map<String, Object>> unlockCourse(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return ResponseEntity.notFound().build();

        course.setMarkingActive(false);
        course.setMarkingLockedBy(null);
        courseRepository.save(course);

        return ResponseEntity.ok(Map.of("unlocked", true));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/attendance/mark
    // Bulk mark attendance for a course on a date
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/mark")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @Transactional
    public ResponseEntity<Map<String, Object>> markBulkAttendance(
            @RequestBody Map<String, Object> payload,
            Authentication auth) {

        try {
            Long courseId = Long.valueOf(payload.get("courseId").toString());
            String dateStr = payload.get("date").toString();
            LocalDate date = LocalDate.parse(dateStr);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> records = (List<Map<String, Object>>) payload.get("records");

            if (records == null || records.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No attendance records provided"));
            }

            Course course = courseRepository.findById(courseId).orElseThrow();
            User marker = userRepository.findByEmail(auth.getName()).orElseThrow();

            int saved = 0;
            for (Map<String, Object> rec : records) {
                Long studentId = Long.valueOf(rec.get("studentId").toString());
                String statusStr = rec.get("status").toString().toUpperCase();
                Attendance.AttendanceStatus status = Attendance.AttendanceStatus.valueOf(statusStr);

                StudentProfile student = studentProfileRepository.findById(studentId).orElse(null);
                if (student == null) continue;

                // Upsert: update if exists, create if not
                Attendance att = attendanceRepository
                        .findByStudentIdAndCourseIdAndDate(studentId, courseId, date)
                        .orElse(new Attendance());

                att.setStudent(student);
                att.setCourse(course);
                att.setDate(date);
                att.setStatus(status);
                att.setMarkedBy(marker);
                attendanceRepository.save(att);
                saved++;
            }

            // Auto-unlock after saving
            course.setMarkingActive(false);
            course.setMarkingLockedBy(null);
            courseRepository.save(course);

            return ResponseEntity.ok(Map.of("saved", saved, "date", dateStr));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Internal error"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/attendance/report/student/{studentId}
    // Student's own attendance history (secured)
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/report/student/{studentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getStudentReport(
            @PathVariable Long studentId,
            Authentication auth) {

        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        boolean isAdminOrTrainer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_TRAINER"));

        // Students can only view their own records
        if (!isAdminOrTrainer) {
            StudentProfile sp = studentProfileRepository.findByUserId(user.getId()).orElse(null);
            if (sp == null || !sp.getId().equals(studentId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }
        }

        List<Attendance> records = attendanceRepository.findByStudentId(studentId);
        long totalDays = records.size();
        long presentDays = records.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT)
                .count();
        long lateDays = records.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.LATE)
                .count();
        double percentage = totalDays > 0 ? (presentDays * 100.0) / totalDays : 0.0;

        // Build daily log
        List<Map<String, Object>> logs = new ArrayList<>();
        for (Attendance a : records) {
            Map<String, Object> log = new HashMap<>();
            log.put("id", a.getId());
            log.put("date", a.getDate().toString());
            log.put("status", a.getStatus().name());
            log.put("courseName", a.getCourse() != null ? a.getCourse().getName() : "—");
            logs.add(log);
        }
        logs.sort((a, b) -> b.get("date").toString().compareTo(a.get("date").toString()));

        Map<String, Object> result = new HashMap<>();
        result.put("studentId", studentId);
        result.put("totalDays", totalDays);
        result.put("presentDays", presentDays);
        result.put("lateDays", lateDays);
        result.put("absentDays", totalDays - presentDays - lateDays);
        result.put("percentage", Math.round(percentage * 10.0) / 10.0);
        result.put("logs", logs);

        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/attendance/report/summary?courseId=X
    // Admin/Trainer: class-wide summary for a course
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/report/summary")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<Map<String, Object>> getCourseSummaryReport(
            @RequestParam Long courseId) {

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return ResponseEntity.notFound().build();

        // Collect enrolled students via FeeRecord (primary) + ManyToMany (fallback)
        Set<Long> addedIds = new HashSet<>();
        List<StudentProfile> enrolledStudents = new ArrayList<>();

        for (FeeRecord fr : feeRecordRepository.findByCourseId(courseId)) {
            if (fr.getStudent() != null && addedIds.add(fr.getStudent().getId())) {
                enrolledStudents.add(fr.getStudent());
            }
        }
        for (StudentProfile sp : studentProfileRepository.findAll()) {
            if (addedIds.contains(sp.getId())) continue;
            boolean inJoinTable = sp.getEnrolledCourses() != null &&
                    sp.getEnrolledCourses().stream().anyMatch(c -> c.getId().equals(courseId));
            if (inJoinTable && addedIds.add(sp.getId())) enrolledStudents.add(sp);
        }

        List<Map<String, Object>> studentStats = new ArrayList<>();
        for (StudentProfile sp : enrolledStudents) {
            long total   = attendanceRepository.countTotalByStudentAndCourse(sp.getId(), courseId);
            long present = attendanceRepository.countPresentByStudentAndCourse(sp.getId(), courseId);
            double pct   = total > 0 ? (present * 100.0) / total : 0.0;

            Map<String, Object> stat = new HashMap<>();
            stat.put("studentId",    sp.getId());
            stat.put("studentName",  sp.getUser() != null ? sp.getUser().getName()  : "Unknown");
            stat.put("email",        sp.getUser() != null ? sp.getUser().getEmail() : "");
            stat.put("totalDays",    total);
            stat.put("presentDays",  present);
            stat.put("percentage",   Math.round(pct * 10.0) / 10.0);
            stat.put("lowAttendance", pct < 75.0);
            studentStats.add(stat);
        }

        List<LocalDate> dates = attendanceRepository.findDistinctDatesByCourse(courseId);
        double avgPct = studentStats.isEmpty() ? 0.0 :
                studentStats.stream().mapToDouble(s -> (Double) s.get("percentage")).average().orElse(0.0);

        Map<String, Object> result = new HashMap<>();
        result.put("courseId",           courseId);
        result.put("courseName",         course.getName());
        result.put("totalStudents",      studentStats.size());
        result.put("averagePercentage",  Math.round(avgPct * 10.0) / 10.0);
        result.put("students",           studentStats);
        result.put("dates",              dates);
        result.put("lowAttendanceCount", studentStats.stream().filter(s -> (Boolean) s.get("lowAttendance")).count());

        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/attendance/course/{courseId}/date/{date}
    // Get attendance for a specific course + date (for editing)
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/course/{courseId}/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public ResponseEntity<List<Map<String, Object>>> getAttendanceForDate(
            @PathVariable Long courseId,
            @PathVariable String date) {

        LocalDate localDate = LocalDate.parse(date);
        List<Attendance> records = attendanceRepository.findByCourseIdAndDate(courseId, localDate);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Attendance a : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("studentId", a.getStudent().getId());
            map.put("studentName", a.getStudent().getUser() != null ? a.getStudent().getUser().getName() : "Unknown");
            map.put("status", a.getStatus().name());
            map.put("date", a.getDate().toString());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Legacy endpoints (kept for backward compatibility)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/student/{studentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getStudentAttendance(
            @PathVariable Long studentId,
            Authentication auth) {
        return getStudentReport(studentId, auth);
    }
}
