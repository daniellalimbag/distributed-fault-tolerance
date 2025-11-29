package com.example.enrollment.web.controller;

import com.example.enrollment.grpc.GetGradesRequest;
import com.example.enrollment.grpc.GradesServiceGrpc;
import com.example.enrollment.grpc.UploadGradeRequest;
import com.example.enrollment.grpc.StudentServiceGrpc;
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

    @GrpcClient("grade")
    private GradesServiceGrpc.GradesServiceBlockingStub gradesStub;

    @GrpcClient("course")
    private StudentServiceGrpc.StudentServiceBlockingStub studentStub;

    @GetMapping("/grades")
    public String viewGrades(HttpSession session, Model model) {
        String studentId = String.valueOf(session.getAttribute("username"));
        Object role = session.getAttribute("role");
        if (role == null || !"STUDENT".equals(role.toString())) return "redirect:/dashboard";
        if (studentId == null) return "redirect:/login";
        var resp = studentStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .getStudentOverview(GetGradesRequest.newBuilder().setStudentId(studentId).build());
        model.addAttribute("grades", resp.getEntriesList());
        return "grades";
    }

    @GetMapping("/upload-grade")
    public String uploadGradeForm(HttpSession session, Model model) {
        return "redirect:/dashboard";
    }

    @PostMapping("/upload-grade")
    public String doUpload(@RequestParam String studentId,
                           @RequestParam String courseId,
                           @RequestParam String grade,
                           HttpSession session) {
        return "redirect:/dashboard";
    }
}
