package com.example.enrollment.service;

import com.example.enrollment.grpc.Course;
import com.example.enrollment.grpc.CourseServiceGrpc;
import com.example.enrollment.grpc.ListCoursesRequest;
import com.example.enrollment.grpc.ListCoursesResponse;
import com.example.enrollment.grpc.CreateCourseRequest;
import com.example.enrollment.grpc.CreateCourseResponse;
import com.example.enrollment.grpc.ListFacultyCoursesRequest;
import com.example.enrollment.grpc.ListFacultyCoursesResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import io.grpc.Context;
import io.grpc.Status;
import static com.example.enrollment.security.ContextKeys.ROLE;
import static com.example.enrollment.security.ContextKeys.SUBJECT;

@GrpcService
public class CourseServiceImpl extends CourseServiceGrpc.CourseServiceImplBase {
    private final List<Course> courses = new ArrayList<>();

    public CourseServiceImpl() {
        courses.add(Course.newBuilder().setId("CS101").setName("Intro to CS").setUnits(3).setLaboratory(false).setFacultyId("f_anything").build());
        courses.add(Course.newBuilder().setId("CS102").setName("Data Structures").setUnits(4).setLaboratory(true).setFacultyId("f_anything").build());
        courses.add(Course.newBuilder().setId("MATH101").setName("Calculus I").setUnits(3).setLaboratory(false).setFacultyId("f_anything").build());
    }

    @Override
    public void listCourses(ListCoursesRequest request, StreamObserver<ListCoursesResponse> responseObserver) {
        responseObserver.onNext(ListCoursesResponse.newBuilder().addAllCourses(courses).build());
        responseObserver.onCompleted();
    }

     public List<Course> getAllCourses() {
         return courses;
     }

     public Optional<Course> findById(String id) {
         return courses.stream().filter(c -> c.getId().equals(id)).findFirst();
     }

    public String getFacultyOfCourse(String courseId) {
        return findById(courseId).map(Course::getFacultyId).orElse(null);
    }

    @Override
    public void createCourse(CreateCourseRequest request, StreamObserver<CreateCourseResponse> responseObserver) {
        String role = ROLE.get(Context.current());
        String facultyId = SUBJECT.get(Context.current());
        if (!"FACULTY".equals(role)) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Only faculty can create courses").asRuntimeException());
            return;
        }
        if (findById(request.getId()).isPresent()) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription("Course ID already exists").asRuntimeException());
            return;
        }
        Course c = Course.newBuilder()
                .setId(request.getId())
                .setName(request.getName())
                .setUnits(request.getUnits())
                .setLaboratory(request.getLaboratory())
                .setFacultyId(facultyId)
                .build();
        courses.add(c);
        responseObserver.onNext(CreateCourseResponse.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void listFacultyCourses(ListFacultyCoursesRequest request, StreamObserver<ListFacultyCoursesResponse> responseObserver) {
        String role = ROLE.get(Context.current());
        String subject = SUBJECT.get(Context.current());
        if (!"FACULTY".equals(role) || !subject.equals(request.getFacultyId())) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Cannot view other faculty's courses").asRuntimeException());
            return;
        }
        var list = courses.stream().filter(c -> request.getFacultyId().equals(c.getFacultyId())).collect(Collectors.toList());
        responseObserver.onNext(ListFacultyCoursesResponse.newBuilder().addAllCourses(list).build());
        responseObserver.onCompleted();
    }
}
