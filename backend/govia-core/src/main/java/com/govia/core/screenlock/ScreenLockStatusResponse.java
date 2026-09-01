package com.govia.core.screenlock;

import java.time.Instant;
import java.util.UUID;

public record ScreenLockStatusResponse(
        String screenKey,
        boolean locked,
        UUID lockedByUserId,
        String lockedByName,
        Instant lockedAt
) {

    public static ScreenLockStatusResponse unlocked(String screenKey) {
        return new ScreenLockStatusResponse(screenKey, false, null, null, null);
    }

    public static ScreenLockStatusResponse of(ScreenLock lock) {
        return new ScreenLockStatusResponse(lock.getScreenKey(), true, lock.getLockedByUserId(), lock.getLockedByName(), lock.getLockedAt());
    }
}
