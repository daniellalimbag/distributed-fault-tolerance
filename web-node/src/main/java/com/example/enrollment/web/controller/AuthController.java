package com.example.enrollment.web.controller;

import com.example.enrollment.grpc.AuthServiceGrpc;
import com.example.enrollment.grpc.LoginRequest;
import com.example.enrollment.grpc.LogoutRequest;
import com.example.enrollment.grpc.RegisterRequest;
import com.example.enrollment.grpc.RegisterResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

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

        if (username.trim().isEmpty() || password.trim().isEmpty()) {
            model.addAttribute("error", "Username and password must not be empty.");
            return "login";
        }

        try {
            var resp = authStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                    .login(LoginRequest.newBuilder().setUsername(username).setPassword(password).build());

            session.setAttribute("token", resp.getToken());
            session.setAttribute("username", username);
            session.setAttribute("role", resp.getRole());

            return "redirect:/dashboard";

        } catch (Exception e) {
            model.addAttribute("error", "Invalid username or password.");
            return "login";
        }
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

    @GetMapping("/register")
    public String registerForm(HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");

        if (!"ADMIN".equals(role)) {
            return "redirect:/dashboard";
        }

        return "register";
    }

    @PostMapping("/register")
    public String doRegister(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(defaultValue = "STUDENT") String role,
            HttpSession session,
            Model model) {

        try {
            String token = (String) session.getAttribute("token");
            String currentRole = (String) session.getAttribute("role");

            if (token == null || !"ADMIN".equals(currentRole)) {
                model.addAttribute("error", "Only ADMIN can register new users.");
                return "login";
            }

            // Build Authorization header
            Metadata headers = new Metadata();
            Metadata.Key<String> AUTH_HEADER =
                    Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
            headers.put(AUTH_HEADER, "Bearer " + token);

            // Attach header to stub
            var securedStub = authStub.withInterceptors(
                    MetadataUtils.newAttachHeadersInterceptor(headers)
            );

            // Call register()
            RegisterResponse response = securedStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                    .register(RegisterRequest.newBuilder()
                            .setUsername(username)
                            .setPassword(password)
                            .setRole(role)
                            .build());

            model.addAttribute("message", "User registered successfully!");
            return "register";

        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

}
