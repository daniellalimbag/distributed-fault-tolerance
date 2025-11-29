package com.example.enrollment.web.controller;

import com.example.enrollment.grpc.GetGradesRequest;
import com.example.enrollment.grpc.GradesServiceGrpc;
import com.example.enrollment.grpc.UploadGradeRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

@Controller
public class GradesController {

    @GrpcClient("api")
    private GradesServiceGrpc.GradesServiceBlockingStub gradesStub;

    @GetMapping("/grades")
    public String viewGrades(HttpSession session, Model model) {
        String studentId = String.valueOf(session.getAttribute("username"));
        if (studentId == null) return "redirect:/login";
        var resp = gradesStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .getGrades(GetGradesRequest.newBuilder().setStudentId(studentId).build());
        model.addAttribute("grades", resp.getGradesList());
        return "grades";
    }

    @GetMapping("/upload-grade")
    public String uploadGradeForm(HttpSession session, Model model) {
        Object role = session.getAttribute("role");
        if (role == null || !"FACULTY".equals(role.toString())) return "redirect:/dashboard";
        return "upload";
    }

    @PostMapping("/upload-grade")
    public String doUpload(@RequestParam String studentId,
                           @RequestParam String courseId,
                           @RequestParam String grade) {
        gradesStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .uploadGrade(UploadGradeRequest.newBuilder()
                .setStudentId(studentId)
                .setCourseId(courseId)
                .setGrade(grade)
                .build());
        return "redirect:/grades";
    }
}
