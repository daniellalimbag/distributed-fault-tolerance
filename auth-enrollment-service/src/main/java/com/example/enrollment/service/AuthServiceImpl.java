package com.example.enrollment.service;

import com.example.enrollment.grpc.AuthServiceGrpc;
import com.example.enrollment.grpc.LoginRequest;
import com.example.enrollment.grpc.LoginResponse;
import com.example.enrollment.grpc.LogoutRequest;
import com.example.enrollment.grpc.LogoutResponse;
import com.example.enrollment.grpc.RegisterRequest;
import com.example.enrollment.grpc.RegisterResponse;
import com.example.enrollment.security.JwtUtil;
import com.example.enrollment.data.UserEntity;
import com.example.enrollment.data.UserRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final UserRepository users;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthServiceImpl(UserRepository users) { this.users = users; }

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        try {
            String id = request.getUsername();
            String rawPassword = request.getPassword();

            UserEntity user = users.findById(id).orElse(null);

            if (user == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
                responseObserver.onError(
                        io.grpc.Status.UNAUTHENTICATED
                                .withDescription("Invalid login credentials")
                                .asRuntimeException()
                );
                return;
            }

            String token = JwtUtil.generateToken(id, user.getRole());
            sessions.put(token, user.getRole());

            LoginResponse resp = LoginResponse.newBuilder()
                    .setToken(token)
                    .setRole(user.getRole())
                    .build();

            responseObserver.onNext(resp);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Error during login: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        boolean removed = sessions.remove(request.getToken()) != null;
        responseObserver.onNext(LogoutResponse.newBuilder()
                .setSuccess(removed)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        try {
            String id = request.getUsername();
            String password = request.getPassword();
            String role = request.getRole().isBlank() ? "STUDENT" : request.getRole();

            if (id == null || id.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
                responseObserver.onError(
                        io.grpc.Status.INVALID_ARGUMENT
                                .withDescription("ID and password must not be empty")
                                .asRuntimeException()
                );
                return;
            }

            if (users.existsById(id)) {
                responseObserver.onError(
                        io.grpc.Status.ALREADY_EXISTS
                                .withDescription("ID already in use")
                                .asRuntimeException()
                );
                return;
            }

            UserEntity newUser = new UserEntity();
            newUser.setId(id);
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setRole(role);

            users.save(newUser);

            RegisterResponse response = RegisterResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("User registered successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Registration failed: " + e.getMessage())
                            .asRuntimeException()
            );
        }
    }

    public boolean registerUser(String id, String password, String role) {
        if (users.existsById(id)) return false;

        UserEntity user = new UserEntity();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);

        users.save(user);
        return true;
    }

    public String loginUser(String id, String rawPassword) {
        UserEntity user = users.findById(id).orElse(null);

        if (user == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Invalid login");
        }

        return JwtUtil.generateToken(id, user.getRole());
    }
}
