package com.example.enrollment.service;

import com.example.enrollment.grpc.Course;
import com.example.enrollment.grpc.CourseServiceGrpc;
import com.example.enrollment.grpc.ListCoursesRequest;
import com.example.enrollment.grpc.ListCoursesResponse;
import com.example.enrollment.grpc.CreateCourseRequest;
import com.example.enrollment.grpc.CreateCourseResponse;
import com.example.enrollment.grpc.ListFacultyCoursesRequest;
import com.example.enrollment.grpc.ListFacultyCoursesResponse;
import com.example.enrollment.data.CourseEntity;
import com.example.enrollment.data.CourseRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import io.grpc.Context;
import io.grpc.Status;
import static com.example.enrollment.security.ContextKeys.ROLE;
import static com.example.enrollment.security.ContextKeys.SUBJECT;

@GrpcService
public class CourseServiceImpl extends CourseServiceGrpc.CourseServiceImplBase {
    private final CourseRepository courseRepository;

    public CourseServiceImpl(CourseRepository courses) { this.courseRepository = courses; }

    @Override
    public void listCourses(ListCoursesRequest request, StreamObserver<ListCoursesResponse> responseObserver) {
        List<Course> all = courseRepository.findAll().stream().map(this::toProto).collect(Collectors.toList());
        responseObserver.onNext(ListCoursesResponse.newBuilder().addAllCourses(all).build());
        responseObserver.onCompleted();
    }

    public List<Course> getAllCourses() { return courseRepository.findAll().stream().map(this::toProto).collect(Collectors.toList()); }

    public Optional<Course> findById(String id) {
        return courseRepository.findById(id).map(this::toProto);
    }

    public String getFacultyOfCourse(String courseId) {
        return courseRepository.findById(courseId).map(CourseEntity::getFacultyId).orElse(null);
    }

    @Override
    public void createCourse(CreateCourseRequest request, StreamObserver<CreateCourseResponse> responseObserver) {
        String role = ROLE.get(Context.current());
        String currentUser = SUBJECT.get(Context.current());    

        if (!"ADMIN".equals(role)) {
            responseObserver.onError(Status.PERMISSION_DENIED
                .withDescription("Only admin can create courses")
                .asRuntimeException());
            return;
        }
        if (courseRepository.existsById(request.getId())) {
            responseObserver.onError(Status.ALREADY_EXISTS
                .withDescription("A course with this ID already exists!")
                .asRuntimeException());
            return;
        }
        if (courseRepository.existsByNameIgnoreCase(request.getName())) {
            responseObserver.onError(Status.ALREADY_EXISTS
                .withDescription("A course with this name already exists!")
                .asRuntimeException());
            return;
        }
        String facultyId = request.getFacultyId();

        if (facultyId == null || facultyId.isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("Faculty ID is required for course assignment")
                .asRuntimeException());
            return;
        }
        courseRepository.save(new CourseEntity(
            request.getId(),
            request.getName(),
            request.getUnits(),
            request.getLaboratory(),
            facultyId
        ));

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
        var list = courseRepository.findByFacultyId(request.getFacultyId()).stream().map(this::toProto).collect(Collectors.toList());
        responseObserver.onNext(ListFacultyCoursesResponse.newBuilder().addAllCourses(list).build());
        responseObserver.onCompleted();
    }

    private Course toProto(CourseEntity e) {
        return Course.newBuilder()
                .setId(e.getId())
                .setName(e.getName())
                .setUnits(e.getUnits())
                .setLaboratory(e.getLaboratory())
                .setFacultyId(e.getFacultyId())
                .build();
    }
}
