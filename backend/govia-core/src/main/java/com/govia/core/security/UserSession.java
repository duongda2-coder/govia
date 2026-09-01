package com.govia.core.security;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** 1 dong = 1 phien dang nhap (1 access/refresh token pair) con "song" hoac da bi thu hoi.
 * jti (JWT ID) la khoa noi voi claim trong token - JwtAuthenticationFilter tra bang nay moi
 * request de biet token da bi da (REVOKED) hay chua, thay vi tin tuong mu quang chu ky con hop le. */
@Getter
@Setter
@Entity
@Table(name = "user_session")
public class UserSession extends BaseEntity {

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "jti", nullable = false, length = 64)
    private String jti;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserSessionStatus status = UserSessionStatus.ACTIVE;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}
