package com.example.enrollment.service;

import com.example.enrollment.grpc.GradeEntry;
import com.example.enrollment.grpc.GetGradesRequest;
import com.example.enrollment.grpc.GetGradesResponse;
import com.example.enrollment.grpc.GradesServiceGrpc;
import com.example.enrollment.grpc.UploadGradeRequest;
import com.example.enrollment.grpc.UploadGradeResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class GradesServiceImpl extends GradesServiceGrpc.GradesServiceImplBase {
    private final Map<String, List<GradeEntry>> grades = new ConcurrentHashMap<>();

    @Override
    public void getGrades(GetGradesRequest request, StreamObserver<GetGradesResponse> responseObserver) {
        List<GradeEntry> list = grades.getOrDefault(request.getStudentId(), Collections.emptyList());
        responseObserver.onNext(GetGradesResponse.newBuilder().addAllGrades(list).build());
        responseObserver.onCompleted();
    }

    @Override
    public void uploadGrade(UploadGradeRequest request, StreamObserver<UploadGradeResponse> responseObserver) {
        grades.computeIfAbsent(request.getStudentId(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(GradeEntry.newBuilder()
                        .setCourseId(request.getCourseId())
                        .setCourseName(request.getCourseId())
                        .setGrade(request.getGrade())
                        .build());
        responseObserver.onNext(UploadGradeResponse.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }
}
