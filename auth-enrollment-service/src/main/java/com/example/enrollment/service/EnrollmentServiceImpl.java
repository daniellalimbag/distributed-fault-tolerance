package com.example.enrollment.service;

import com.example.enrollment.grpc.EnrollRequest;
import com.example.enrollment.grpc.EnrollResponse;
import com.example.enrollment.grpc.EnrollmentServiceGrpc;
import com.example.enrollment.grpc.ListStudentsInCourseRequest;
import com.example.enrollment.grpc.ListStudentsInCourseResponse;
import com.example.enrollment.grpc.StudentCourseGrade;
import com.example.enrollment.data.EnrollmentEntity;
import com.example.enrollment.data.EnrollmentId;
import com.example.enrollment.data.EnrollmentRepository;
import com.example.enrollment.data.CourseRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.*;
import io.grpc.Context;
import io.grpc.Status;
import com.example.enrollment.security.ContextKeys;
import org.springframework.transaction.annotation.Transactional;

@GrpcService
public class EnrollmentServiceImpl extends EnrollmentServiceGrpc.EnrollmentServiceImplBase {
    private final EnrollmentRepository enrollments;
    private final CourseRepository courses;

    public EnrollmentServiceImpl(EnrollmentRepository enrollments, CourseRepository courses) {
        this.enrollments = enrollments;
        this.courses = courses;
    }

@Override
@Transactional
public void drop(EnrollRequest request, StreamObserver<EnrollResponse> responseObserver) {
    String role = ContextKeys.ROLE.get(Context.current());
    String subject = ContextKeys.SUBJECT.get(Context.current());

    if (!"STUDENT".equals(role) || !request.getStudentId().equals(subject)) {
        responseObserver.onError(Status.PERMISSION_DENIED
            .withDescription("Only the student can drop their own courses")
            .asRuntimeException());
        return;
    }

    enrollments.findByStudentIdAndCourseId(request.getStudentId(), request.getCourseId())
        .ifPresentOrElse(enrollment -> {
            enrollments.delete(enrollment);
            enrollments.flush(); // ensure deletion hits DB
            responseObserver.onNext(EnrollResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Course dropped successfully")
                .build());
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(Status.NOT_FOUND
                .withDescription("Student is not enrolled in this course")
                .asRuntimeException());
        });
}



    @Override
    @Transactional
    public void enroll(EnrollRequest request, StreamObserver<EnrollResponse> responseObserver) {
        String role = ContextKeys.ROLE.get(Context.current());
        String subject = ContextKeys.SUBJECT.get(Context.current());
        if (!"STUDENT".equals(role) || !request.getStudentId().equals(subject)) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Only the student can enroll themselves").asRuntimeException());
            return;
        }
        if (!courses.existsById(request.getCourseId())) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Course not found").asRuntimeException());
            return;
        }
        // Enforce capacity: closed when capacity reached
        var courseOpt = courses.findById(request.getCourseId());
        if (courseOpt.isEmpty()) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Course not found").asRuntimeException());
            return;
        }
        var course = courseOpt.get();
        long current = enrollments.countByCourseId(request.getCourseId());
        Integer capacity = course.getCapacity();
        if (capacity != null && current >= capacity) {
            responseObserver.onError(Status.RESOURCE_EXHAUSTED.withDescription("Course is full").asRuntimeException());
            return;
        }
        EnrollmentId id = new EnrollmentId(request.getStudentId(), request.getCourseId());
        enrollments.findById(id).orElseGet(() -> enrollments.save(new EnrollmentEntity(request.getStudentId(), request.getCourseId(), null)));
        responseObserver.onNext(EnrollResponse.newBuilder().setSuccess(true).setMessage("Enrolled").build());
        responseObserver.onCompleted();
    }

    @Override
    public void listStudentsInCourse(ListStudentsInCourseRequest request, StreamObserver<ListStudentsInCourseResponse> responseObserver) {
        String role = ContextKeys.ROLE.get(Context.current());
        String facultyId = ContextKeys.SUBJECT.get(Context.current());
        if (!"FACULTY".equals(role)) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Only faculty can list students").asRuntimeException());
            return;
        }
        var owner = courses.findById(request.getCourseId()).map(e -> e.getFacultyId()).orElse(null);
        if (owner == null || !owner.equals(facultyId)) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Not owner of course").asRuntimeException());
            return;
        }
        List<StudentCourseGrade> entries = new ArrayList<>();
        enrollments.findByCourseId(request.getCourseId()).forEach(en -> {
            String gradeVal = en.getGrade() == null ? "null" : en.getGrade();
            entries.add(StudentCourseGrade.newBuilder().setStudentId(en.getStudentId()).setGrade(gradeVal).build());
        });
        responseObserver.onNext(ListStudentsInCourseResponse.newBuilder().addAllEntries(entries).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getEnrollmentCount(com.example.enrollment.grpc.GetEnrollmentCountRequest request,
                                   io.grpc.stub.StreamObserver<com.example.enrollment.grpc.GetEnrollmentCountResponse> responseObserver) {
        // Accessible to any authenticated role; just return the count for the course
        long count = enrollments.countByCourseId(request.getCourseId());
        responseObserver.onNext(com.example.enrollment.grpc.GetEnrollmentCountResponse.newBuilder()
                .setCount((int) count)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void completeCourse(com.example.enrollment.grpc.CompleteCourseRequest request,
                               io.grpc.stub.StreamObserver<com.example.enrollment.grpc.CompleteCourseResponse> responseObserver) {
        String role = ContextKeys.ROLE.get(Context.current());
        if (!"ADMIN".equals(role)) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Only admin can complete courses").asRuntimeException());
            return;
        }

        // validate academic_year_range format YYYY-YYYY and continuity
        String yr = request.getAcademicYearRange();
        if (!yr.matches("^\\d{4}-\\d{4}$")) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("academic_year_range must be YYYY-YYYY").asRuntimeException());
            return;
        }
        int start = Integer.parseInt(yr.substring(0, 4));
        int end = Integer.parseInt(yr.substring(5, 9));
        if (end != start + 1) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("academic_year_range must be consecutive years").asRuntimeException());
            return;
        }
        int termNumber = request.getTermNumber();
        if (termNumber < 1 || termNumber > 3) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("term_number must be 1-3").asRuntimeException());
            return;
        }

        // mark all current enrollments in this course as completed if not already
        var list = enrollments.findByCourseId(request.getCourseId());
        int affected = 0;
        java.time.Instant now = java.time.Instant.now();
        for (var en : list) {
            if (en.getCompletedAt() == null) {
                en.setTermNumber((short) termNumber);
                en.setAcademicYearRange(yr);
                en.setCompletedAt(now);
                affected++;
            }
        }
        if (affected > 0) {
            enrollments.saveAll(list);
        }
        responseObserver.onNext(com.example.enrollment.grpc.CompleteCourseResponse.newBuilder().setAffected(affected).build());
        responseObserver.onCompleted();
    }
}
