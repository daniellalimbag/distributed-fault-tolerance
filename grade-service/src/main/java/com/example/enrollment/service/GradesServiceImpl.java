package com.example.enrollment.service;

import com.example.enrollment.grpc.GradeEntry;
import com.example.enrollment.grpc.GetGradesRequest;
import com.example.enrollment.grpc.GetGradesResponse;
import com.example.enrollment.grpc.GradesServiceGrpc;
import com.example.enrollment.grpc.UploadGradeRequest;
import com.example.enrollment.grpc.UploadGradeResponse;
import com.example.enrollment.data.EnrollmentRepository;
import com.example.enrollment.data.EnrollmentEntity;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.*;
import io.grpc.Context;
import io.grpc.Status;
import com.example.enrollment.security.ContextKeys;
import org.springframework.transaction.annotation.Transactional;

@GrpcService
public class GradesServiceImpl extends GradesServiceGrpc.GradesServiceImplBase {
    private final EnrollmentRepository enrollments;

    public GradesServiceImpl(EnrollmentRepository enrollments) { this.enrollments = enrollments; }

    @Override
    public void getGrades(GetGradesRequest request, StreamObserver<GetGradesResponse> responseObserver) {
        String role = ContextKeys.ROLE.get(Context.current());
        String subject = ContextKeys.SUBJECT.get(Context.current());
        if (!"STUDENT".equals(role) || !request.getStudentId().equals(subject)) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Students can only view their own grades").asRuntimeException());
            return;
        }
        var list = new ArrayList<GradeEntry>();
        enrollments.findByStudentId(request.getStudentId()).forEach(en -> {
            String courseName = en.getCourseId();
            String gradeVal = en.getGrade() == null ? "NGS" : en.getGrade();
            list.add(GradeEntry.newBuilder()
                    .setCourseId(en.getCourseId())
                    .setCourseName(courseName)
                    .setGrade(gradeVal)
                    .build());
        });
        responseObserver.onNext(GetGradesResponse.newBuilder().addAllGrades(list).build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void uploadGrade(UploadGradeRequest request, StreamObserver<UploadGradeResponse> responseObserver) {
        String role = ContextKeys.ROLE.get(Context.current());
        if (!"FACULTY".equals(role)) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Only faculty can upload grades").asRuntimeException());
            return;
        }
        var opt = enrollments.findByStudentIdAndCourseId(request.getStudentId(), request.getCourseId());
        if (opt.isEmpty()) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Enrollment not found").asRuntimeException());
            return;
        }
        EnrollmentEntity en = opt.get();
        en.setGrade(request.getGrade());
        enrollments.save(en);
        responseObserver.onNext(UploadGradeResponse.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }
}
