package com.example.enrollment.web.grpc;

import io.grpc.*;
import jakarta.servlet.http.HttpSession;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcGlobalClientInterceptor
@Order(0)
public class AuthClientInterceptor implements ClientInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AuthClientInterceptor.class);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions,
                                                               Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attr != null) {
                    HttpSession session = attr.getRequest().getSession(false);
                    if (session != null) {
                        Object token = session.getAttribute("token");
                        if (token != null) {
                            headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + token);
                            if (log.isDebugEnabled()) {
                                log.debug("Attached Authorization header for method {}", method.getFullMethodName());
                            }
                        } else if (log.isDebugEnabled()) {
                            log.debug("No token in session for method {}", method.getFullMethodName());
                        }
                    } else if (log.isDebugEnabled()) {
                        log.debug("No HTTP session for method {}", method.getFullMethodName());
                    }
                } else if (log.isDebugEnabled()) {
                    log.debug("No request attributes; cannot attach Authorization for method {}", method.getFullMethodName());
                }
                super.start(responseListener, headers);
            }
        };
    }
}
