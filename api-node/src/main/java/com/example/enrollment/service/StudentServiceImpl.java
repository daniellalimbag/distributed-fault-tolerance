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
import java.util.Set;

import static com.example.enrollment.security.ContextKeys.ROLE;
import static com.example.enrollment.security.ContextKeys.SUBJECT;

@GrpcService
public class StudentServiceImpl extends StudentServiceGrpc.StudentServiceImplBase {

    private final EnrollmentStore store;
    private final CourseServiceImpl courseService;

    public StudentServiceImpl(EnrollmentStore store, CourseServiceImpl courseService) {
        this.store = store;
        this.courseService = courseService;
    }

    @Override
    public void getStudentOverview(GetGradesRequest request, StreamObserver<GetStudentOverviewResponse> responseObserver) {
        String role = ROLE.get(Context.current());
        String subject = SUBJECT.get(Context.current());
        if (!"STUDENT".equals(role) || !request.getStudentId().equals(subject)) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Students can only view their own overview").asRuntimeException());
            return;
        }
        Set<String> enrolled = store.getEnrollments().getOrDefault(request.getStudentId(), java.util.Collections.emptySet());
        List<GradeEntry> grades = store.getGrades().getOrDefault(request.getStudentId(), java.util.Collections.emptyList());
        List<GradeEntry> entries = new ArrayList<>();
        for (String courseId : enrolled) {
            String courseName = courseService.findById(courseId).map(c -> c.getName()).orElse(courseId);
            String gradeVal = "NGS";
            for (GradeEntry g : grades) {
                if (g.getCourseId().equals(courseId)) { gradeVal = g.getGrade(); break; }
            }
            entries.add(GradeEntry.newBuilder()
                    .setCourseId(courseId)
                    .setCourseName(courseName)
                    .setGrade(gradeVal)
                    .build());
        }
        responseObserver.onNext(GetStudentOverviewResponse.newBuilder().addAllEntries(entries).build());
        responseObserver.onCompleted();
    }
}
