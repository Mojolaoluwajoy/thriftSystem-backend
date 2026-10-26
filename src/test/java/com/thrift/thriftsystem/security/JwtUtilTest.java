package com.thrift.thriftsystem.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

    // 64 random-looking bytes, Base64-encoded, as JWT_SECRET must be
    private static final String SECRET =
            Base64.getEncoder().encodeToString("0123456789abcdef".repeat(4).getBytes());

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 86_400_000L);
    }

    @Test
    void generateToken_RoundTripsEmailAndExpiresInFuture() {
        String token = jwtUtil.generateToken("ada@example.com");

        assertTrue(jwtUtil.validateToken(token));
        assertEquals("ada@example.com", jwtUtil.extractEmail(token));
        long expiresInMs =
                jwtUtil.extractClaims(token).getExpiration().getTime() - System.currentTimeMillis();
        assertTrue(expiresInMs > 86_000_000L && expiresInMs <= 86_400_000L);
    }

    @Test
    void validateToken_RejectsExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", -1_000L);

        assertFalse(jwtUtil.validateToken(jwtUtil.generateToken("ada@example.com")));
    }

    @Test
    void validateToken_RejectsTokenSignedWithAnotherSecret() {
        String token = jwtUtil.generateToken("ada@example.com");
        ReflectionTestUtils.setField(
                jwtUtil,
                "jwtSecret",
                Base64.getEncoder().encodeToString("fedcba9876543210".repeat(4).getBytes()));

        assertFalse(jwtUtil.validateToken(token));
    }
}
