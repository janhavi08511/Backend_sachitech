package com.example.sachitech.controller;

import com.example.sachitech.entity.Assignment;
import com.example.sachitech.entity.StudentProfile;
import com.example.sachitech.repository.AssignmentRepository;
import com.example.sachitech.repository.AttendanceRepository;
import com.example.sachitech.repository.CourseRepository;
import com.example.sachitech.repository.FeeRecordRepository;
import com.example.sachitech.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Performance Analytics endpoints consumed by PerformanceModule.tsx
 *
 * GET /api/performance/student/{studentId}/course/{courseId}
 *     → { averageScore, classRank, attendancePercentage }
 *
 * GET /api/performance/student/{studentId}/skills
 *     → [ { skillName, skillRating } ]
 *
 * GET /api/performance/course/{courseId}/leaderboard
 *     → [ { studentName, averageScore } ]
 *
 * GET /api/performance/student/{studentId}/course/{courseId}/insight
 *     → plain string
 */
@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final AssignmentRepository    assignmentRepository;
    private final AttendanceRepository    attendanceRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CourseRepository        courseRepository;
    private final FeeRecordRepository     feeRecordRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Student performance summary for a specific course
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/student/{studentId}/course/{courseId}")
    public ResponseEntity<Map<String, Object>> getPerformance(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {

        // Average score from evaluated assignments
        List<Assignment> assignments = assignmentRepository.findByStudentId(studentId)
                .stream()
                .filter(a -> a.getCourse() != null && a.getCourse().getId().equals(courseId))
                .filter(a -> a.getScore() != null && a.getMaxScore() != null && a.getMaxScore() > 0)
                .toList();

        double averageScore = assignments.isEmpty() ? 0.0 :
                assignments.stream()
                        .mapToDouble(a -> (a.getScore() / a.getMaxScore()) * 100.0)
                        .average()
                        .orElse(0.0);

        // Attendance percentage
        long present = attendanceRepository.countPresentByStudentAndCourse(studentId, courseId);
        long total   = attendanceRepository.countTotalByStudentAndCourse(studentId, courseId);
        double attendancePct = total > 0 ? Math.round((present * 100.0 / total) * 10) / 10.0 : 0.0;

        // Class rank: count students in this course who scored higher
        List<Assignment> allCourseAssignments = assignmentRepository.findByCourseId(courseId)
                .stream()
                .filter(a -> a.getScore() != null && a.getMaxScore() != null && a.getMaxScore() > 0)
                .toList();

        Map<Long, Double> studentAvgMap = allCourseAssignments.stream()
                .filter(a -> a.getStudent() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getStudent().getId(),
                        Collectors.averagingDouble(a -> (a.getScore() / a.getMaxScore()) * 100.0)
                ));

        long rank = studentAvgMap.values().stream()
                .filter(score -> score > averageScore)
                .count() + 1;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("averageScore",          Math.round(averageScore * 10) / 10.0);
        result.put("classRank",             rank);
        result.put("attendancePercentage",  attendancePct);
        result.put("totalAssignments",      assignments.size());
        result.put("totalClasses",          total);
        result.put("classesAttended",       present);

        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Skills breakdown — derived from per-assignment scores across all courses
    // Each assignment title becomes a "skill"; score % becomes the rating
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/student/{studentId}/skills")
    public ResponseEntity<List<Map<String, Object>>> getSkills(@PathVariable Long studentId) {

        List<Assignment> assignments = assignmentRepository.findByStudentId(studentId)
                .stream()
                .filter(a -> a.getScore() != null && a.getMaxScore() != null && a.getMaxScore() > 0)
                .toList();

        if (assignments.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Group by assignment title → average score % as skill rating (0–10 scale)
        Map<String, Double> skillMap = assignments.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getTitle() != null ? a.getTitle() : "General",
                        Collectors.averagingDouble(a -> (a.getScore() / a.getMaxScore()) * 10.0)
                ));

        List<Map<String, Object>> skills = skillMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> skill = new LinkedHashMap<>();
                    skill.put("skillName",   e.getKey());
                    skill.put("skillRating", Math.round(e.getValue() * 10) / 10.0);
                    return skill;
                })
                .sorted(Comparator.comparingDouble(m -> -((Double) m.get("skillRating"))))
                .toList();

        return ResponseEntity.ok(skills);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Leaderboard for a course — top students by average assignment score
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/course/{courseId}/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard(@PathVariable Long courseId) {

        List<Assignment> assignments = assignmentRepository.findByCourseId(courseId)
                .stream()
                .filter(a -> a.getStudent() != null)
                .filter(a -> a.getScore() != null && a.getMaxScore() != null && a.getMaxScore() > 0)
                .toList();

        if (assignments.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Group by student → average score %
        Map<Long, List<Assignment>> byStudent = assignments.stream()
                .collect(Collectors.groupingBy(a -> a.getStudent().getId()));

        List<Map<String, Object>> leaderboard = byStudent.entrySet().stream()
                .map(e -> {
                    StudentProfile sp = e.getValue().get(0).getStudent();
                    String name = (sp.getUser() != null) ? sp.getUser().getName() : "Student " + sp.getId();
                    double avg = e.getValue().stream()
                            .mapToDouble(a -> (a.getScore() / a.getMaxScore()) * 100.0)
                            .average().orElse(0.0);

                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("studentId",    sp.getId());
                    entry.put("studentName",  name);
                    entry.put("averageScore", Math.round(avg * 10) / 10.0);
                    return entry;
                })
                .sorted(Comparator.comparingDouble(m -> -((Double) m.get("averageScore"))))
                .toList();

        return ResponseEntity.ok(leaderboard);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI-style insight — rule-based text derived from real metrics
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/student/{studentId}/course/{courseId}/insight")
    public ResponseEntity<String> getInsight(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {

        // Reuse the same calculations
        List<Assignment> assignments = assignmentRepository.findByStudentId(studentId)
                .stream()
                .filter(a -> a.getCourse() != null && a.getCourse().getId().equals(courseId))
                .filter(a -> a.getScore() != null && a.getMaxScore() != null && a.getMaxScore() > 0)
                .toList();

        double avgScore = assignments.isEmpty() ? 0.0 :
                assignments.stream()
                        .mapToDouble(a -> (a.getScore() / a.getMaxScore()) * 100.0)
                        .average().orElse(0.0);

        long present = attendanceRepository.countPresentByStudentAndCourse(studentId, courseId);
        long total   = attendanceRepository.countTotalByStudentAndCourse(studentId, courseId);
        double attendancePct = total > 0 ? (present * 100.0 / total) : 0.0;

        String insight = buildInsight(avgScore, attendancePct, assignments.size());
        return ResponseEntity.ok(insight);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: rule-based insight text
    // ─────────────────────────────────────────────────────────────────────────

    private String buildInsight(double avgScore, double attendancePct, int assignmentCount) {
        if (assignmentCount == 0 && attendancePct == 0) {
            return "No data available yet for this student in this course. Enroll them and start tracking assignments and attendance.";
        }

        StringBuilder sb = new StringBuilder();

        // Score insight
        if (avgScore >= 85) {
            sb.append("Excellent performance with an average score of ")
              .append(String.format("%.1f", avgScore)).append("%. ");
        } else if (avgScore >= 65) {
            sb.append("Good performance with an average score of ")
              .append(String.format("%.1f", avgScore)).append("%. ");
        } else if (avgScore > 0) {
            sb.append("Needs improvement — current average score is ")
              .append(String.format("%.1f", avgScore)).append("%. ");
        }

        // Attendance insight
        if (attendancePct >= 90) {
            sb.append("Attendance is excellent at ").append(String.format("%.0f", attendancePct)).append("%. ");
        } else if (attendancePct >= 75) {
            sb.append("Attendance is satisfactory at ").append(String.format("%.0f", attendancePct)).append("%. ");
        } else if (attendancePct > 0) {
            sb.append("Attendance is below the 75% threshold at ")
              .append(String.format("%.0f", attendancePct)).append("% — immediate improvement required. ");
        }

        // Recommendation
        if (avgScore < 65 && attendancePct < 75) {
            sb.append("Recommend scheduling a counselling session.");
        } else if (avgScore < 65) {
            sb.append("Focus on assignment quality and revision.");
        } else if (attendancePct < 75) {
            sb.append("Prioritise regular attendance to avoid academic penalties.");
        } else {
            sb.append("Keep up the good work!");
        }

        return sb.toString().trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/performance/student/{studentId}/all-courses
    // Full breakdown across every course the student is enrolled in:
    // attendance %, assignment grades, submission status
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/student/{studentId}/all-courses")
    public ResponseEntity<List<Map<String, Object>>> getAllCoursesPerformance(
            @PathVariable Long studentId) {

        // Enrolled courses = courses with a FeeRecord for this student
        List<com.example.sachitech.entity.FeeRecord> feeRecords =
                feeRecordRepository.findByStudentId(studentId);

        // Also include courses from ManyToMany join table
        StudentProfile sp = studentProfileRepository.findById(studentId).orElse(null);
        Set<Long> courseIds = new LinkedHashSet<>();
        feeRecords.forEach(fr -> { if (fr.getCourse() != null) courseIds.add(fr.getCourse().getId()); });
        if (sp != null && sp.getEnrolledCourses() != null) {
            sp.getEnrolledCourses().forEach(c -> courseIds.add(c.getId()));
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Long courseId : courseIds) {
            var courseOpt = courseRepository.findById(courseId);
            if (courseOpt.isEmpty()) continue;
            var course = courseOpt.get();

            // Attendance
            long totalClasses  = attendanceRepository.countTotalByStudentAndCourse(studentId, courseId);
            long presentClasses = attendanceRepository.countPresentByStudentAndCourse(studentId, courseId);
            double attendancePct = totalClasses > 0
                    ? Math.round((presentClasses * 100.0 / totalClasses) * 10) / 10.0 : 0.0;

            // Assignments for this course
            List<Assignment> assignments = assignmentRepository.findByStudentId(studentId)
                    .stream()
                    .filter(a -> a.getCourse() != null && a.getCourse().getId().equals(courseId))
                    .toList();

            List<Map<String, Object>> assignmentList = assignments.stream().map(a -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",               a.getId());
                m.put("title",            a.getTitle() != null ? a.getTitle() : "Untitled");
                m.put("maxScore",         a.getMaxScore() != null ? a.getMaxScore() : 0);
                m.put("score",            a.getScore());
                m.put("percentage",       (a.getScore() != null && a.getMaxScore() != null && a.getMaxScore() > 0)
                                          ? Math.round((a.getScore() / a.getMaxScore()) * 1000) / 10.0 : null);
                m.put("evaluationStatus", a.getEvaluationStatus() != null ? a.getEvaluationStatus() : "PENDING");
                m.put("submissionLink",   a.getStudentSubmissionLink());
                return m;
            }).toList();

            double avgScore = assignments.stream()
                    .filter(a -> a.getScore() != null && a.getMaxScore() != null && a.getMaxScore() > 0)
                    .mapToDouble(a -> (a.getScore() / a.getMaxScore()) * 100.0)
                    .average().orElse(0.0);

            long submitted = assignments.stream()
                    .filter(a -> a.getStudentSubmissionLink() != null && !a.getStudentSubmissionLink().isBlank())
                    .count();

            Map<String, Object> courseData = new LinkedHashMap<>();
            courseData.put("courseId",          courseId);
            courseData.put("courseName",        course.getName());
            courseData.put("attendancePercentage", attendancePct);
            courseData.put("totalClasses",      totalClasses);
            courseData.put("classesAttended",   presentClasses);
            courseData.put("averageScore",      Math.round(avgScore * 10) / 10.0);
            courseData.put("totalAssignments",  assignments.size());
            courseData.put("submitted",         submitted);
            courseData.put("assignments",       assignmentList);
            courseData.put("insight",           buildInsight(avgScore, attendancePct, assignments.size()));
            result.add(courseData);
        }

        return ResponseEntity.ok(result);
    }
}
