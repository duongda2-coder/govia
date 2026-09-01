package com.govia.core.security;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Quan ly phien dang nhap (UserSession) - nen tang cho tinh nang "phat hien dang nhap dong thoi
 * o may khac" va "da phien cu ra". Moi lan login thanh cong tao 1 dong ACTIVE gan voi 1 jti rieng;
 * JwtAuthenticationFilter tra bang nay moi request de biet token con hieu luc hay da bi REVOKED. */
@Service
public class UserSessionService {

    private final UserSessionRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    public UserSessionService(UserSessionRepository repository, SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public UserSession create(UUID tenantId, UUID userId, String jti, String deviceInfo, String ipAddress) {
        UserSession session = new UserSession();
        session.setTenantId(tenantId);
        session.setUserId(userId);
        session.setJti(jti);
        session.setDeviceInfo(deviceInfo);
        session.setIpAddress(ipAddress);
        session.setStatus(UserSessionStatus.ACTIVE);
        session.setLastSeenAt(Instant.now());
        return repository.save(session);
    }

    @Transactional(readOnly = true)
    public List<UserSession> findActive(UUID tenantId, UUID userId) {
        return repository.findByTenantIdAndUserIdAndStatus(tenantId, userId, UserSessionStatus.ACTIVE);
    }

    /** True neu jti con la 1 phien ACTIVE - dung boi JwtAuthenticationFilter tren MOI request de
     * phat hien token da bi da (REVOKED boi 1 lan login khac) truoc khi het han tu nhien. */
    @Transactional(readOnly = true)
    public boolean isActive(String jti) {
        return repository.findByJti(jti).map(s -> s.getStatus() == UserSessionStatus.ACTIVE).orElse(false);
    }

    /** Da toan bo phien ACTIVE hien tai cua user nay ra (khi nguoi dung chon "dang xuat phien
     * kia") - moi phien bi da nhan 1 message realtime qua WebSocket (queue rieng theo jti) de
     * frontend hien thong bao + chuyen ve trang login ngay, khong can doi token het han/goi API
     * tiep theo moi phat hien. */
    @Transactional
    public void revokeAll(UUID tenantId, UUID userId) {
        List<UserSession> sessions = findActive(tenantId, userId);
        Instant now = Instant.now();
        for (UserSession session : sessions) {
            session.setStatus(UserSessionStatus.REVOKED);
            session.setRevokedAt(now);
            repository.save(session);
            messagingTemplate.convertAndSendToUser(session.getJti(), "/queue/session-kicked",
                    Map.of("message", "Tai khoan da dang nhap o noi khac."));
        }
    }

    @Transactional
    public void revokeByJti(String jti) {
        repository.findByJti(jti).ifPresent(session -> {
            session.setStatus(UserSessionStatus.REVOKED);
            session.setRevokedAt(Instant.now());
            repository.save(session);
        });
    }

    @Transactional
    public void touch(String jti) {
        repository.findByJti(jti).ifPresent(session -> {
            session.setLastSeenAt(Instant.now());
            repository.save(session);
        });
    }
}
