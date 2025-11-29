package com.example.enrollment.security;

import io.grpc.Context;

public final class ContextKeys {
    private ContextKeys() {}
    public static final Context.Key<String> SUBJECT = Context.key("subject");
    public static final Context.Key<String> ROLE = Context.key("role");
}
