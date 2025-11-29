package com.example.enrollment.service;

import com.example.enrollment.grpc.EnrollRequest;
import com.example.enrollment.grpc.EnrollResponse;
import com.example.enrollment.grpc.EnrollmentServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class EnrollmentServiceImpl extends EnrollmentServiceGrpc.EnrollmentServiceImplBase {
    private final Map<String, Set<String>> enrollments = new ConcurrentHashMap<>();

    @Override
    public void enroll(EnrollRequest request, StreamObserver<EnrollResponse> responseObserver) {
        enrollments.computeIfAbsent(request.getStudentId(), k -> ConcurrentHashMap.newKeySet())
                .add(request.getCourseId());
        responseObserver.onNext(EnrollResponse.newBuilder().setSuccess(true).setMessage("Enrolled").build());
        responseObserver.onCompleted();
    }
}
