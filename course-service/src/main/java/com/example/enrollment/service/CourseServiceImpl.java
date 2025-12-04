package com.example.enrollment.service;

import com.example.enrollment.grpc.Course;
import com.example.enrollment.grpc.CourseServiceGrpc;
import com.example.enrollment.grpc.ListCoursesRequest;
import com.example.enrollment.grpc.ListCoursesResponse;
import com.example.enrollment.grpc.CreateCourseRequest;
import com.example.enrollment.grpc.CreateCourseResponse;
import com.example.enrollment.grpc.DeleteCourseResponse;
import com.example.enrollment.grpc.DeleteCourseRequest;
import com.example.enrollment.grpc.ListFacultyCoursesRequest;
import com.example.enrollment.grpc.ListFacultyCoursesResponse;
import com.example.enrollment.grpc.UpdateCourseRequest;
import com.example.enrollment.grpc.UpdateCourseResponse;


import com.example.enrollment.data.CourseEntity;
import com.example.enrollment.data.CourseRepository;
import com.example.enrollment.data.EnrollmentRepository;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.grpc.Context;
import io.grpc.Status;

import static com.example.enrollment.security.ContextKeys.ROLE;
import static com.example.enrollment.security.ContextKeys.SUBJECT;

import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class CourseServiceImpl extends CourseServiceGrpc.CourseServiceImplBase {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    // FIXED CONSTRUCTOR — now inject both repositories
    @Autowired
    public CourseServiceImpl(CourseRepository courseRepository,
                             EnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    public void listCourses(ListCoursesRequest request, StreamObserver<ListCoursesResponse> responseObserver) {
        List<Course> all = courseRepository.findAll().stream()
                .map(this::toProto)
                .collect(Collectors.toList());

        responseObserver.onNext(
                ListCoursesResponse.newBuilder()
                        .addAllCourses(all)
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void updateCourseCapacity(com.example.enrollment.grpc.UpdateCourseCapacityRequest request,
                                     StreamObserver<com.example.enrollment.grpc.UpdateCourseCapacityResponse> responseObserver) {
        String role = ROLE.get(Context.current());
        if (!"ADMIN".equals(role)) {
            responseObserver.onError(Status.PERMISSION_DENIED
                    .withDescription("Only admin can update capacity").asRuntimeException());
            return;
        }

        var opt = courseRepository.findById(request.getId());
        if (opt.isEmpty()) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Course not found").asRuntimeException());
            return;
        }

        var entity = opt.get();
        entity.setCapacity(request.getCapacity());
        courseRepository.save(entity);

        responseObserver.onNext(
                com.example.enrollment.grpc.UpdateCourseCapacityResponse.newBuilder()
                        .setSuccess(true).build()
        );
        responseObserver.onCompleted();
    }


    public List<Course> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(this::toProto)
                .collect(Collectors.toList());
    }

    public Optional<Course> findById(String id) {
        return courseRepository.findById(id).map(this::toProto);
    }

    public String getFacultyOfCourse(String courseId) {
        return courseRepository.findById(courseId)
                .map(CourseEntity::getFacultyId)
                .orElse(null);
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

        int capacity = request.getCapacity() == 0 ? 30 : request.getCapacity();

        courseRepository.save(new CourseEntity(
                request.getId(),
                request.getName(),
                request.getUnits(),
                request.getLaboratory(),
                facultyId,
                capacity
        ));

        responseObserver.onNext(CreateCourseResponse.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }


    @Override
    public void deleteCourse(DeleteCourseRequest request, StreamObserver<DeleteCourseResponse> responseObserver) {
        String role = ROLE.get(Context.current());
        String currentUser = SUBJECT.get(Context.current());

        if (!"ADMIN".equals(role)) {
            responseObserver.onError(Status.PERMISSION_DENIED
                    .withDescription("Only admin can delete courses")
                    .asRuntimeException());
            return;
        }

        String courseId = request.getCourseId();
        if (courseId == null || courseId.isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("CourseId is required")
                    .asRuntimeException());
            return;
        }

        if (!courseRepository.existsById(courseId)) {
            DeleteCourseResponse resp = DeleteCourseResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Course does not exist")
                    .build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
            return;
        }

        // Check enrollments before deleting
        var enrollments = enrollmentRepository.findByCourseId(courseId);

        if (enrollments != null && !enrollments.isEmpty()) {
            DeleteCourseResponse resp = DeleteCourseResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Cannot delete course: there are " + enrollments.size() +
                                " enrollment(s). Remove or drop them first.")
                    .build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
            return;
        }

        // Safe to delete
        courseRepository.deleteById(courseId);

        DeleteCourseResponse resp = DeleteCourseResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Course deleted")
                .build();

        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void updateCourse(UpdateCourseRequest request, StreamObserver<UpdateCourseResponse> responseObserver) {
        String role = ROLE.get(Context.current());
        if (!"ADMIN".equals(role)) {
            responseObserver.onError(Status.PERMISSION_DENIED
                    .withDescription("Only admin can update courses")
                    .asRuntimeException());
            return;
        }

        String id = request.getId();
        if (id == null || id.isBlank()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Course ID is required")
                    .asRuntimeException());
            return;
        }

        var opt = courseRepository.findById(id);
        if (opt.isEmpty()) {
            UpdateCourseResponse resp = UpdateCourseResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Course not found")
                    .build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
            return;
        }

        var entity = opt.get();

        // Optional: prevent name collision with other courses
        // If you want to enforce unique name:
        // if (!entity.getName().equalsIgnoreCase(request.getName())
        //         && courseRepository.existsByNameIgnoreCase(request.getName())) {
        //     UpdateCourseResponse resp = UpdateCourseResponse.newBuilder()
        //             .setSuccess(false)
        //             .setMessage("Another course with this name already exists")
        //             .build();
        //     responseObserver.onNext(resp);
        //     responseObserver.onCompleted();
        //     return;
        // }

        entity.setName(request.getName());
        entity.setUnits(request.getUnits());
        entity.setLaboratory(request.getLaboratory());
        entity.setFacultyId(request.getFacultyId());
        entity.setCapacity(request.getCapacity());

        courseRepository.save(entity);

        UpdateCourseResponse resp = UpdateCourseResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Course updated")
                .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }



    @Override
    public void listFacultyCourses(ListFacultyCoursesRequest request, StreamObserver<ListFacultyCoursesResponse> responseObserver) {
        String role = ROLE.get(Context.current());
        String subject = SUBJECT.get(Context.current());

        if (!"FACULTY".equals(role) || !subject.equals(request.getFacultyId())) {
            responseObserver.onError(Status.PERMISSION_DENIED
                    .withDescription("Cannot view other faculty's courses")
                    .asRuntimeException());
            return;
        }

        var list = courseRepository.findByFacultyId(request.getFacultyId())
                .stream()
                .map(this::toProto)
                .collect(Collectors.toList());

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
                .setCapacity(e.getCapacity() == null ? 0 : e.getCapacity())
                .build();
    }
}
