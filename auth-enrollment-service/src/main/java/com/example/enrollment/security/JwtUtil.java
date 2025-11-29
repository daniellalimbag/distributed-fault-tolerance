package com.example.enrollment.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.nio.charset.StandardCharsets;

public class JwtUtil {
    private static final Key KEY = initKey();
    private static final long EXP_MS = 24 * 60 * 60 * 1000L;

    public static String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .addClaims(Map.of("role", role))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXP_MS))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims parseAndValidate(String token) throws JwtException {
        return Jwts.parserBuilder().setSigningKey(KEY).build().parseClaimsJws(token).getBody();
    }

    public static String getRole(Claims claims) {
        Object role = claims.get("role");
        return role == null ? null : role.toString();
    }

    private static Key initKey() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
        // Use provided secret (plain text) for HMAC key
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
