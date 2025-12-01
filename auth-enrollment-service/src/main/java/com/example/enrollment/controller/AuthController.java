package com.example.enrollment.controller;

import com.example.enrollment.service.AuthServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> req) {
        String id = req.get("username");
        String password = req.get("password");
        String role = req.getOrDefault("role", "STUDENT");

        boolean success = authService.registerUser(id, password, role);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "User registered successfully" : "ID already in use"
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req) {
        String id = req.get("username");
        String password = req.get("password");

        try {
            String token = authService.loginUser(id, password);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception ex) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid login credentials"));
        }
    }
}
