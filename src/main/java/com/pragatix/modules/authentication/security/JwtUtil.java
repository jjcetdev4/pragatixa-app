package com.pragatix.modules.authentication.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for JWT token generation, validation, and parsing.
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.student-expiration}")
    private long studentExpiration;

    // ── Token Generation ─────────────────────────────

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "USER");
        return buildToken(claims, userDetails.getUsername(), expiration);
    }

    public String generateStudentToken(String regNo, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "STUDENT");
        claims.put("email", email);
        return buildToken(claims, regNo, studentExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiryMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Token Validation ─────────────────────────────

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isStudentTokenValid(String token, String regNo) {
        try {
            final String subject = extractUsername(token);
            final String type = extractClaim(token, claims -> claims.get("type", String.class));
            return subject.equals(regNo) && "STUDENT".equals(type) && !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("Invalid student token: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ── Claims Extraction ────────────────────────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        // Base64-encode the secret string to produce a valid key
        String encoded = Base64.getEncoder().encodeToString(secret.getBytes());
        byte[] keyBytes = Decoders.BASE64.decode(encoded);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
