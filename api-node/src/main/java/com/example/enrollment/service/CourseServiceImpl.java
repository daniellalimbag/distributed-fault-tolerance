package com.example.enrollment.service;

import com.example.enrollment.grpc.Course;
import com.example.enrollment.grpc.CourseServiceGrpc;
import com.example.enrollment.grpc.ListCoursesRequest;
import com.example.enrollment.grpc.ListCoursesResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;

@GrpcService
public class CourseServiceImpl extends CourseServiceGrpc.CourseServiceImplBase {
    private final List<Course> courses = new ArrayList<>();

    public CourseServiceImpl() {
        courses.add(Course.newBuilder().setId("CS101").setName("Intro to CS").setUnits(3).setLaboratory(false).build());
        courses.add(Course.newBuilder().setId("CS102").setName("Data Structures").setUnits(4).setLaboratory(true).build());
        courses.add(Course.newBuilder().setId("MATH101").setName("Calculus I").setUnits(3).setLaboratory(false).build());
    }

    @Override
    public void listCourses(ListCoursesRequest request, StreamObserver<ListCoursesResponse> responseObserver) {
        responseObserver.onNext(ListCoursesResponse.newBuilder().addAllCourses(courses).build());
        responseObserver.onCompleted();
    }
}
