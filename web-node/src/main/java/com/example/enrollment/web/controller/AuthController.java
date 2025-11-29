package com.example.enrollment.web.controller;

import com.example.enrollment.grpc.AuthServiceGrpc;
import com.example.enrollment.grpc.LoginRequest;
import com.example.enrollment.grpc.LogoutRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

@Controller
public class AuthController {

    @GrpcClient("authEnroll")
    private AuthServiceGrpc.AuthServiceBlockingStub authStub;

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpSession session,
                          Model model) {
        var resp = authStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .login(LoginRequest.newBuilder().setUsername(username).setPassword(password).build());
        session.setAttribute("token", resp.getToken());
        session.setAttribute("username", username);
        session.setAttribute("role", resp.getRole());
        return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        Object token = session.getAttribute("token");
        if (token != null) {
            authStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                    .logout(LogoutRequest.newBuilder().setToken(token.toString()).build());
        }
        session.invalidate();
        return "redirect:/login";
    }
}
