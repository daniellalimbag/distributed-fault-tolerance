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

    // Only the student can drop their own course
    if (!"STUDENT".equals(role) || !request.getStudentId().equals(subject)) {
        responseObserver.onError(Status.PERMISSION_DENIED
                .withDescription("Only the student can drop their own courses")
                .asRuntimeException());
        return;
    }

    List<EnrollmentEntity> enrolled = enrollments.findByStudentId(request.getStudentId());
    if (enrolled.isEmpty()) {
        responseObserver.onError(
                Status.NOT_FOUND
                        .withDescription("Student is not enrolled in any courses")
                        .asRuntimeException()
        );
        return;
    }

    EnrollmentEntity target = null;
    for (EnrollmentEntity e : enrolled) {
        if (e.getCourseId().equals(request.getCourseId())) {
            target = e;
            break;
        }
    }

        if (target == null) {
        responseObserver.onError(
                Status.NOT_FOUND
                        .withDescription("The student is not enrolled in this course")
                        .asRuntimeException()
        );
        return;
    }

    enrollments.delete(target);

    responseObserver.onNext(EnrollResponse.newBuilder()
            .setSuccess(true)
            .setMessage("Course dropped successfully")
            .build());
    responseObserver.onCompleted();
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
}
