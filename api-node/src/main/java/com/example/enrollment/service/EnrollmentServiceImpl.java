package com.example.enrollment.service;

import com.example.enrollment.grpc.EnrollRequest;
import com.example.enrollment.grpc.EnrollResponse;
import com.example.enrollment.grpc.EnrollmentServiceGrpc;
import com.example.enrollment.grpc.ListStudentsInCourseRequest;
import com.example.enrollment.grpc.ListStudentsInCourseResponse;
import com.example.enrollment.grpc.StudentCourseGrade;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import io.grpc.Context;
import io.grpc.Status;
import com.example.enrollment.security.ContextKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@GrpcService
public class EnrollmentServiceImpl extends EnrollmentServiceGrpc.EnrollmentServiceImplBase {
    private final EnrollmentStore store;
    private final CourseServiceImpl courseService;

    public EnrollmentServiceImpl(EnrollmentStore store, CourseServiceImpl courseService) {
        this.store = store;
        this.courseService = courseService;
    }

    @Override
    public void enroll(EnrollRequest request, StreamObserver<EnrollResponse> responseObserver) {
        String role = ContextKeys.ROLE.get(Context.current());
        String subject = ContextKeys.SUBJECT.get(Context.current());
        if (!"STUDENT".equals(role) || !request.getStudentId().equals(subject)) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Only the student can enroll themselves").asRuntimeException());
            return;
        }
        store.getEnrollments().computeIfAbsent(request.getStudentId(), k -> ConcurrentHashMap.newKeySet())
                .add(request.getCourseId());
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
        String owner = courseService.getFacultyOfCourse(request.getCourseId());
        if (owner == null || !owner.equals(facultyId)) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Not owner of course").asRuntimeException());
            return;
        }
        List<StudentCourseGrade> entries = new ArrayList<>();
        store.getEnrollments().forEach((student, courseSet) -> {
            if (courseSet.contains(request.getCourseId())) {
                String gradeVal = "null";
                var list = store.getGrades().get(student);
                if (list != null) {
                    for (var g : list) {
                        if (g.getCourseId().equals(request.getCourseId())) { gradeVal = g.getGrade(); break; }
                    }
                }
                entries.add(StudentCourseGrade.newBuilder()
                        .setStudentId(student)
                        .setGrade(gradeVal)
                        .build());
            }
        });
        responseObserver.onNext(ListStudentsInCourseResponse.newBuilder().addAllEntries(entries).build());
        responseObserver.onCompleted();
    }
}
