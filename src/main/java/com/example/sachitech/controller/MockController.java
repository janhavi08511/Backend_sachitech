package com.example.sachitech.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
public class MockController {

    @GetMapping("/lms/courses")
    public List<java.util.Map<String, Object>> getCourses() {
        return List.of(
            java.util.Map.of("id", 1, "name", "Full Stack Java", "title", "Full Stack Java"),
            java.util.Map.of("id", 2, "name", "MERN Stack", "title", "MERN Stack"),
            java.util.Map.of("id", 3, "name", "Python Backend", "title", "Python Backend")
        );
    }

    // /reports/* routes are now handled by ReportController

    @GetMapping("/lms/modules/{courseId}")
    public List<Object> getLMSModules(@org.springframework.web.bind.annotation.PathVariable Long courseId) {
        return java.util.Collections.emptyList();
    }

    @GetMapping("/lms/lessons/{moduleId}")
    public List<Object> getLMSLessons(@org.springframework.web.bind.annotation.PathVariable Long moduleId) {
        return java.util.Collections.emptyList();
    }

    @GetMapping("/lms/assignments/{courseId}")
    public List<Object> getLMSAssignments(@org.springframework.web.bind.annotation.PathVariable Long courseId) {
        return java.util.Collections.emptyList();
    }

    @GetMapping("/fees/payments")
    public List<Object> getFeesPayments() {
        return Collections.emptyList();
    }

    @GetMapping("/fees/stats")
    public Map<String, Object> getFeeStats() {
        return Map.of("totalRevenue", 0, "pendingDues", 0);
    }

    @GetMapping("/internship-placement/internships")
    public List<Map<String, Object>> getInternships() {
        return List.of(
            Map.of("id", 1, "name", "Backend Engineering Intern", "title", "Backend Engineering Intern"),
            Map.of("id", 2, "name", "Frontend React Intern", "title", "Frontend React Intern")
        );
    }

    @GetMapping("/internship-placement/placements")
    public List<Object> getPlacements() {
        return Collections.emptyList();
    }

    // /api/performance/* routes are now handled by PerformanceController

}
