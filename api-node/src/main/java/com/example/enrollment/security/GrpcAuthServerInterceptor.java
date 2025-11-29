package com.example.enrollment.security;

import io.grpc.*;
import io.grpc.Metadata.Key;
import io.jsonwebtoken.JwtException;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.core.annotation.Order;

@GrpcGlobalServerInterceptor
@Order(0)
public class GrpcAuthServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> AUTHORIZATION = Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        String method = call.getMethodDescriptor().getFullMethodName();
        // Allow Login without token
        if (method.endsWith("/Login")) {
            return next.startCall(call, headers);
        }

        String auth = headers.get(AUTHORIZATION);
        if (auth == null || !auth.toLowerCase().startsWith("bearer ")) {
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid Authorization header"), new Metadata());
            return new ServerCall.Listener<>() {};
        }
        String token = auth.substring(7).trim();
        try {
            JwtUtil.parseAndValidate(token);
        } catch (JwtException e) {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid token").withCause(e), new Metadata());
            return new ServerCall.Listener<>() {};
        }
        return next.startCall(call, headers);
    }
}
