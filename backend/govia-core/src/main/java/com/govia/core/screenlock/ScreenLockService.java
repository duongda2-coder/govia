package com.govia.core.screenlock;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Khoa MAN HINH (khong phai tung ban ghi) - khi 1 user dang mo form Them/Sua tren 1 man hinh,
 * khong ai khac (kha ca chinh user do o tab/may khac) duoc Them/Sua/Xoa tren CUNG man hinh do cho
 * toi khi ho dong lai. screenKey la id on dinh cua man hinh (CrudTable.tableId, vd
 * "audit.plan.workItem"). Khoa tu dong het han neu qua STALE_SECONDS khong heartbeat (vd nguoi
 * dung dong tab dot ngot ma khong bam Huy/Luu) - dam bao khong bi "khoa treo" vinh vien. */
@Service
public class ScreenLockService {

    private static final long STALE_SECONDS = 60;

    private final ScreenLockRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    public ScreenLockService(ScreenLockRepository repository, SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    public record AcquireResult(boolean acquired, ScreenLockStatusResponse status) {
    }

    @Transactional
    public AcquireResult acquire(UUID tenantId, String screenKey, UUID userId, String userName) {
        var existing = repository.findByTenantIdAndScreenKey(tenantId, screenKey);
        Instant now = Instant.now();

        if (existing.isPresent()) {
            ScreenLock lock = existing.get();
            boolean sameHolder = lock.getLockedByUserId().equals(userId);
            boolean stale = lock.getLastHeartbeatAt().isBefore(now.minus(STALE_SECONDS, ChronoUnit.SECONDS));
            if (!sameHolder && !stale) {
                return new AcquireResult(false, ScreenLockStatusResponse.of(lock));
            }
            if (!sameHolder) {
                lock.setLockedByUserId(userId);
                lock.setLockedByName(userName);
                lock.setLockedAt(now);
            }
            lock.setLastHeartbeatAt(now);
            repository.save(lock);
            broadcast(ScreenLockStatusResponse.of(lock));
            return new AcquireResult(true, ScreenLockStatusResponse.of(lock));
        }

        ScreenLock lock = new ScreenLock();
        lock.setTenantId(tenantId);
        lock.setScreenKey(screenKey);
        lock.setLockedByUserId(userId);
        lock.setLockedByName(userName);
        lock.setLockedAt(now);
        lock.setLastHeartbeatAt(now);
        repository.save(lock);
        ScreenLockStatusResponse status = ScreenLockStatusResponse.of(lock);
        broadcast(status);
        return new AcquireResult(true, status);
    }

    /** Gia han khoa dinh ky trong khi form van dang mo. Neu khoa da bi nguoi khac chiem (vd khoa
     * cua minh da qua han va bi 1 nguoi khac gianh mat truoc khi kip heartbeat tiep theo), tra ve
     * trang thai moi nhat de FE tu dong dong form + bao loi thay vi tiep tuc sua nham. */
    @Transactional
    public ScreenLockStatusResponse heartbeat(UUID tenantId, String screenKey, UUID userId) {
        var existing = repository.findByTenantIdAndScreenKey(tenantId, screenKey);
        if (existing.isEmpty()) {
            return ScreenLockStatusResponse.unlocked(screenKey);
        }
        ScreenLock lock = existing.get();
        if (!lock.getLockedByUserId().equals(userId)) {
            return ScreenLockStatusResponse.of(lock);
        }
        lock.setLastHeartbeatAt(Instant.now());
        repository.save(lock);
        return ScreenLockStatusResponse.of(lock);
    }

    @Transactional
    public void release(UUID tenantId, String screenKey, UUID userId) {
        repository.findByTenantIdAndScreenKey(tenantId, screenKey)
                .filter(lock -> lock.getLockedByUserId().equals(userId))
                .ifPresent(lock -> {
                    repository.delete(lock);
                    broadcast(ScreenLockStatusResponse.unlocked(screenKey));
                });
    }

    @Transactional(readOnly = true)
    public ScreenLockStatusResponse getStatus(UUID tenantId, String screenKey) {
        return repository.findByTenantIdAndScreenKey(tenantId, screenKey)
                .filter(lock -> lock.getLastHeartbeatAt().isAfter(Instant.now().minus(STALE_SECONDS, ChronoUnit.SECONDS)))
                .map(ScreenLockStatusResponse::of)
                .orElse(ScreenLockStatusResponse.unlocked(screenKey));
    }

    private void broadcast(ScreenLockStatusResponse status) {
        messagingTemplate.convertAndSend("/topic/screen-lock." + status.screenKey(), status);
    }
}
