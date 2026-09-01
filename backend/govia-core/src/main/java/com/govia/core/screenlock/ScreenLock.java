package com.govia.core.screenlock;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** 1 dong = 1 man hinh (screenKey, thuong la CrudTable.tableId) dang bi 1 user chiem giu de sua -
 * khong ai khac duoc Them/Sua/Xoa tren man hinh do cho toi khi nguoi giu khoa dong lai (release)
 * hoac khoa qua han (STALE_SECONDS khong heartbeat, vd dong tab dot ngot). */
@Getter
@Setter
@Entity
@Table(name = "screen_lock")
public class ScreenLock extends BaseEntity {

    @Column(name = "screen_key", nullable = false, length = 150)
    private String screenKey;

    @Column(name = "locked_by_user_id", nullable = false, columnDefinition = "uuid")
    private UUID lockedByUserId;

    @Column(name = "locked_by_name", length = 255)
    private String lockedByName;

    @Column(name = "locked_at", nullable = false)
    private Instant lockedAt;

    @Column(name = "last_heartbeat_at", nullable = false)
    private Instant lastHeartbeatAt;
}
