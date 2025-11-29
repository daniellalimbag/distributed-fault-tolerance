package com.example.enrollment.security;

import io.grpc.*;
import io.grpc.Metadata.Key;
import io.jsonwebtoken.JwtException;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.core.annotation.Order;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Base64;

@GrpcGlobalServerInterceptor
@Order(0)
public class GrpcAuthServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> AUTHORIZATION = Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    private static final Logger log = LoggerFactory.getLogger(GrpcAuthServerInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        String method = call.getMethodDescriptor().getFullMethodName();
        if (method.endsWith("/Login")) {
            return next.startCall(call, headers);
        }

        String auth = headers.get(AUTHORIZATION);
        if (auth == null || !auth.toLowerCase().startsWith("bearer ")) {
            log.info("UNAUTHENTICATED: missing/invalid Authorization for {}", method);
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid Authorization header"), new Metadata());
            return new ServerCall.Listener<>() {};
        }
        String token = auth.substring(7).trim();
        try {
            try {
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
                    log.info("Incoming JWT header for {}: {}", method, headerJson);
                }
            } catch (Exception ignore) {}
            Claims claims = JwtUtil.parseAndValidate(token);
            String subject = claims.getSubject();
            String role = JwtUtil.getRole(claims);
            log.info("Authenticated {} as subject={}, role={}", method, subject, role);
            Context ctx = Context.current()
                    .withValue(ContextKeys.SUBJECT, subject)
                    .withValue(ContextKeys.ROLE, role);
            return Contexts.interceptCall(ctx, call, headers, next);
        } catch (JwtException e) {
             log.info("UNAUTHENTICATED: invalid token for {} - {}", method, e.getMessage());
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid token").withCause(e), new Metadata());
            return new ServerCall.Listener<>() {};
        }
    }
}
