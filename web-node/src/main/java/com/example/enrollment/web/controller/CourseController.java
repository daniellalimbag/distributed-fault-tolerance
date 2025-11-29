package com.example.enrollment.web.controller;

import com.example.enrollment.grpc.CourseServiceGrpc;
import com.example.enrollment.grpc.EnrollRequest;
import com.example.enrollment.grpc.EnrollmentServiceGrpc;
import com.example.enrollment.grpc.ListCoursesRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

@Controller
public class CourseController {

    @GrpcClient("api")
    private CourseServiceGrpc.CourseServiceBlockingStub courseStub;

    @GrpcClient("api")
    private EnrollmentServiceGrpc.EnrollmentServiceBlockingStub enrollmentStub;

    @GetMapping("/courses")
    public String listCourses(Model model) {
        var response = courseStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .listCourses(ListCoursesRequest.newBuilder().build());
        model.addAttribute("courses", response.getCoursesList());
        return "courses";
    }

    @PostMapping("/enroll")
    public String enroll(@RequestParam String courseId, HttpSession session) {
        String studentId = String.valueOf(session.getAttribute("username"));
        if (studentId == null) return "redirect:/login";
        enrollmentStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .enroll(EnrollRequest.newBuilder().setStudentId(studentId).setCourseId(courseId).build());
        return "redirect:/courses";
    }
}
