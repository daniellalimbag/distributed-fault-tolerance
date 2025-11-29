package com.example.enrollment.service;

import com.example.enrollment.grpc.GradeEntry;
import com.example.enrollment.grpc.GetGradesRequest;
import com.example.enrollment.grpc.GetStudentOverviewResponse;
import com.example.enrollment.grpc.StudentServiceGrpc;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;
import static com.example.enrollment.security.ContextKeys.ROLE;
import static com.example.enrollment.security.ContextKeys.SUBJECT;
import com.example.enrollment.data.EnrollmentRepository;
import com.example.enrollment.data.CourseRepository;

@GrpcService
public class StudentServiceImpl extends StudentServiceGrpc.StudentServiceImplBase {

    private final EnrollmentRepository enrollments;
    private final CourseRepository courses;

    public StudentServiceImpl(EnrollmentRepository enrollments, CourseRepository courses) {
        this.enrollments = enrollments;
        this.courses = courses;
    }

    @Override
    public void getStudentOverview(GetGradesRequest request, StreamObserver<GetStudentOverviewResponse> responseObserver) {
        String role = ROLE.get(Context.current());
        String subject = SUBJECT.get(Context.current());
        if (!"STUDENT".equals(role) || !request.getStudentId().equals(subject)) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Students can only view their own overview").asRuntimeException());
            return;
        }
        List<GradeEntry> entries = new ArrayList<>();
        enrollments.findByStudentId(request.getStudentId()).forEach(en -> {
            String courseName = courses.findById(en.getCourseId()).map(c -> c.getName()).orElse(en.getCourseId());
            String gradeVal = en.getGrade() == null ? "NGS" : en.getGrade();
            entries.add(GradeEntry.newBuilder()
                    .setCourseId(en.getCourseId())
                    .setCourseName(courseName)
                    .setGrade(gradeVal)
                    .build());
        });
        responseObserver.onNext(GetStudentOverviewResponse.newBuilder().addAllEntries(entries).build());
        responseObserver.onCompleted();
    }
}
