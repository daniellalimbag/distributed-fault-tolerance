package com.example.enrollment.service;

import com.example.enrollment.grpc.GetStudentHistoryRequest;
import com.example.enrollment.grpc.GetStudentHistoryResponse;
import com.example.enrollment.grpc.HistoryEntry;
import com.example.enrollment.grpc.StudentServiceGrpc;
import com.example.enrollment.data.EnrollmentRepository;
import com.example.enrollment.data.CourseRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import io.grpc.Context;
import io.grpc.Status;
import com.example.enrollment.security.ContextKeys;

@GrpcService
public class StudentServiceImpl extends StudentServiceGrpc.StudentServiceImplBase {
    private final EnrollmentRepository enrollments;
    private final CourseRepository courses;

    public StudentServiceImpl(EnrollmentRepository enrollments, CourseRepository courses) {
        this.enrollments = enrollments;
        this.courses = courses;
    }

    @Override
    public void getStudentOverview(com.example.enrollment.grpc.GetGradesRequest request,
                                   io.grpc.stub.StreamObserver<com.example.enrollment.grpc.GetStudentOverviewResponse> responseObserver) {
        String role = ContextKeys.ROLE.get(Context.current());
        String subject = ContextKeys.SUBJECT.get(Context.current());
        if (!"STUDENT".equals(role) || !request.getStudentId().equals(subject)) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Students can only view their own overview").asRuntimeException());
            return;
        }
        var list = new java.util.ArrayList<com.example.enrollment.grpc.GradeEntry>();
        enrollments.findByStudentId(request.getStudentId()).forEach(en -> {
            if (en.getCompletedAt() == null) { // current, not completed
                String courseName = courses.findById(en.getCourseId()).map(c -> c.getName()).orElse(en.getCourseId());
                String gradeVal = en.getGrade() == null ? "NGS" : en.getGrade();
                list.add(com.example.enrollment.grpc.GradeEntry.newBuilder()
                        .setCourseId(en.getCourseId())
                        .setCourseName(courseName)
                        .setGrade(gradeVal)
                        .build());
            }
        });
        responseObserver.onNext(com.example.enrollment.grpc.GetStudentOverviewResponse.newBuilder().addAllEntries(list).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getStudentHistory(GetStudentHistoryRequest request, StreamObserver<GetStudentHistoryResponse> responseObserver) {
        String role = ContextKeys.ROLE.get(Context.current());
        String subject = ContextKeys.SUBJECT.get(Context.current());
        if (!"STUDENT".equals(role) || !request.getStudentId().equals(subject)) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Students can only view their own history").asRuntimeException());
            return;
        }
        var list = new ArrayList<HistoryEntry>();
        var formatter = DateTimeFormatter.ISO_INSTANT;
        enrollments.findByStudentId(request.getStudentId()).forEach(en -> {
            if (en.getCompletedAt() != null) {
                String courseName = courses.findById(en.getCourseId()).map(c -> c.getName()).orElse(en.getCourseId());
                String gradeVal = en.getGrade() == null ? "NGS" : en.getGrade();
                String completedAtStr = formatter.format(en.getCompletedAt().atOffset(ZoneOffset.UTC));
                HistoryEntry entry = HistoryEntry.newBuilder()
                        .setCourseId(en.getCourseId())
                        .setCourseName(courseName)
                        .setGrade(gradeVal)
                        .setTermNumber(en.getTermNumber() == null ? 0 : en.getTermNumber())
                        .setAcademicYearRange(en.getAcademicYearRange() == null ? "" : en.getAcademicYearRange())
                        .setCompletedAt(completedAtStr)
                        .build();
                list.add(entry);
            }
        });
        responseObserver.onNext(GetStudentHistoryResponse.newBuilder().addAllEntries(list).build());
        responseObserver.onCompleted();
    }
}
