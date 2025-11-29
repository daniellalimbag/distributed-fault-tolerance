package com.example.enrollment.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import net.devh.boot.grpc.client.inject.GrpcClient;
import com.example.enrollment.grpc.StudentServiceGrpc;
import com.example.enrollment.grpc.GetGradesRequest;
import java.util.concurrent.TimeUnit;

@Controller
public class DashboardController {

    @GrpcClient("api")
    private StudentServiceGrpc.StudentServiceBlockingStub studentStub;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(HttpSession session, Model model) {
        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("role", session.getAttribute("role"));
        Object role = session.getAttribute("role");
        Object username = session.getAttribute("username");
        if (role != null && "STUDENT".equals(role.toString()) && username != null) {
            var resp = studentStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                    .getStudentOverview(GetGradesRequest.newBuilder().setStudentId(username.toString()).build());
            model.addAttribute("enrollmentCount", resp.getEntriesCount());
        }
        return "dashboard";
    }
}
