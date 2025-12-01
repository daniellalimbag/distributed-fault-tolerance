package com.example.enrollment.security;

import io.grpc.*;
import io.grpc.Metadata.Key;
import io.jsonwebtoken.JwtException;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.core.annotation.Order;
import io.jsonwebtoken.Claims;

@GrpcGlobalServerInterceptor
@Order(0)
public class GrpcAuthServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> AUTHORIZATION = Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        String method = call.getMethodDescriptor().getFullMethodName();

        if (method.endsWith("/Login") || method.endsWith("/Register") || method.endsWith("/Logout")) {
            return next.startCall(call, headers);
        }

        String auth = headers.get(AUTHORIZATION);
        if (auth == null || !auth.toLowerCase().startsWith("bearer ")) {
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid Authorization header"), new Metadata());
            return new ServerCall.Listener<>() {};
        }
        String token = auth.substring(7).trim();
        try {
            Claims claims = JwtUtil.parseAndValidate(token);
            String subject = claims.getSubject();
            String role = JwtUtil.getRole(claims);
            Context ctx = Context.current()
                    .withValue(ContextKeys.SUBJECT, subject)
                    .withValue(ContextKeys.ROLE, role);
            return Contexts.interceptCall(ctx, call, headers, next);
        } catch (JwtException e) {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid token").withCause(e), new Metadata());
            return new ServerCall.Listener<>() {};
        }
    }
}
