package com.lifepath.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final int ACCESS_TOKEN_EXPIRY_SECONDS = 900;  // 15 min
    private static final int REFRESH_TOKEN_EXPIRY_DAYS = 7;

    private final SecretKey accessKey;
    private final SecretKey refreshKey;

    public JwtService(
            @Value("${jwt.access-secret:lifepath-access-secret-min-256-bits-for-hs256-please-change-in-production}") String accessSecret,
            @Value("${jwt.refresh-secret:lifepath-refresh-secret-min-256-bits-for-hs256-please-change-in-production}") String refreshSecret) {
        this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(UUID userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(ACCESS_TOKEN_EXPIRY_SECONDS)))
                .signWith(accessKey)
                .compact();
    }

    public String createRefreshToken(UUID userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRY_DAYS * 24 * 3600)))
                .signWith(refreshKey)
                .compact();
    }

    public UUID parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(accessKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return UUID.fromString(claims.getSubject());
        } catch (JwtException e) {
            throw new JwtAuthenticationException("Invalid or expired access token");
        }
    }

    public UUID parseRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(refreshKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return UUID.fromString(claims.getSubject());
        } catch (JwtException e) {
            throw new JwtAuthenticationException("Invalid or expired refresh token");
        }
    }

    public int getAccessTokenExpirySeconds() {
        return ACCESS_TOKEN_EXPIRY_SECONDS;
    }

    public static class JwtAuthenticationException extends RuntimeException {
        public JwtAuthenticationException(String message) {
            super(message);
        }
    }
}
