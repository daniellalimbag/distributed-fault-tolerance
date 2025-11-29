package com.example.enrollment.service;

import com.example.enrollment.grpc.GradeEntry;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EnrollmentStore {
    private final Map<String, Set<String>> enrollments = new ConcurrentHashMap<>();
    private final Map<String, List<GradeEntry>> grades = new ConcurrentHashMap<>();

    public Map<String, Set<String>> getEnrollments() {
        return enrollments;
    }

    public Map<String, List<GradeEntry>> getGrades() {
        return grades;
    }
}
