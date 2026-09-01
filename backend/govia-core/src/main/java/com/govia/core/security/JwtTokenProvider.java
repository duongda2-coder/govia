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
    private static final String CLAIM_PURPOSE = "purpose";
    private static final String PURPOSE_PENDING_LOGIN = "pending-login";

    private final JwtProperties properties;
    private final SecretKey key;

    @Autowired
    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String username, UUID tenantId, String employeeCode,
                                       List<String> roles, List<String> permissions, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(jti)
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

    public String generateRefreshToken(UUID userId, String username, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(jti)
                .subject(username)
                .claim(CLAIM_USER_ID, userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getRefreshTokenDays(), ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    /** Token ngan han (2 phut) sinh khi phat hien dang nhap dong thoi - xac nhan user da nhap
     * dung mat khau (khong bat go lai) trong khi cho ho chon "da phien cu" hay "dang nhap song
     * song" o buoc /api/auth/login/resolve. */
    public String generatePendingLoginToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim(CLAIM_USER_ID, userId.toString())
                .claim(CLAIM_PURPOSE, PURPOSE_PENDING_LOGIN)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(2, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public UUID parsePendingLoginToken(String token) {
        Claims claims = parseClaims(token);
        if (!PURPOSE_PENDING_LOGIN.equals(claims.get(CLAIM_PURPOSE, String.class))) {
            throw new io.jsonwebtoken.JwtException("Token khong dung muc dich");
        }
        return UUID.fromString(claims.get(CLAIM_USER_ID, String.class));
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
                claims.get(CLAIM_PERMISSIONS, List.class),
                claims.getId()
        );
    }
}
