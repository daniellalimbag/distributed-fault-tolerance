package com.example.enrollment.service;

import com.example.enrollment.grpc.AuthServiceGrpc;
import com.example.enrollment.grpc.LoginRequest;
import com.example.enrollment.grpc.LoginResponse;
import com.example.enrollment.grpc.LogoutRequest;
import com.example.enrollment.grpc.LogoutResponse;
import com.example.enrollment.security.JwtUtil;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        String role = request.getUsername().startsWith("f_") ? "FACULTY" : "STUDENT";
        String token = JwtUtil.generateToken(request.getUsername(), role);
        sessions.put(token, role);
        LoginResponse resp = LoginResponse.newBuilder().setToken(token).setRole(role).build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        boolean removed = sessions.remove(request.getToken()) != null;
        responseObserver.onNext(LogoutResponse.newBuilder().setSuccess(removed).build());
        responseObserver.onCompleted();
    }

    public boolean isValid(String token) {
        return token != null && sessions.containsKey(token);
    }
}
