package com.thrift.thriftsystem.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("jwt token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("jwt token unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("jwt token malformed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("jwt claims empty: {}", e.getMessage());
        } catch (JwtException e) {
            // Includes signature mismatches, e.g. a forged token or one from another secret
            log.error("jwt token invalid: {}", e.getMessage());
        }
        return false;
    }

    public Claims extractClaims(String token) {

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        return (Claims) Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
