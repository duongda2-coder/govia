package com.govia.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Sinh va giai ma JWT dung chung cho toan platform.
 * Tat ca module (Identity, People, Audit...) deu xac thuc bang cung 1 token
 * do govia-identity phat hanh - khong module nao tu lam auth rieng.
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TENANT = "tenantId";
    private static final String CLAIM_EMPLOYEE_CODE = "employeeCode";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_USER_ID = "userId";

    private final JwtProperties properties;
    private final SecretKey key;

    @Autowired
    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String username, UUID tenantId, String employeeCode,
                                       List<String> roles, List<String> permissions) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_USER_ID, userId.toString())
                .claim(CLAIM_TENANT, tenantId.toString())
                .claim(CLAIM_EMPLOYEE_CODE, employeeCode)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_PERMISSIONS, permissions)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAccessTokenMinutes(), ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UUID userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_USER_ID, userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getRefreshTokenDays(), ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @SuppressWarnings("unchecked")
    public CurrentUserPrincipal toPrincipal(Claims claims) {
        return new CurrentUserPrincipal(
                UUID.fromString(claims.get(CLAIM_USER_ID, String.class)),
                claims.getSubject(),
                UUID.fromString(claims.get(CLAIM_TENANT, String.class)),
                claims.get(CLAIM_EMPLOYEE_CODE, String.class),
                claims.get(CLAIM_ROLES, List.class),
                claims.get(CLAIM_PERMISSIONS, List.class)
        );
    }
}
