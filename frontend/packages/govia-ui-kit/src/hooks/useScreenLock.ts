import { useCallback, useEffect, useRef, useState } from "react";
import type { AxiosInstance } from "axios";
import { subscribeTopic } from "../ws/wsClient";
import type { ApiResponse } from "../api/httpClient";

export interface ScreenLockStatus {
  screenKey: string;
  locked: boolean;
  lockedByUserId: string | null;
  lockedByName: string | null;
  lockedAt: string | null;
}

export interface UseScreenLockResult {
  status: ScreenLockStatus;
  /** true neu CHINH nguoi dung hien tai la nguoi dang giu khoa (so sanh lockedByUserId, khong
   * phai suy tu viec instance nay co goi acquire() hay khong - vi CrudTable va trang page co the
   * cung goi hook nay doc lap, chi 1 ben thuc su acquire). */
  isMine: boolean;
  /** true trong luc dang goi acquire/heartbeat/release - dung de disable nut tam thoi, tranh bam
   * kep lien tuc trong luc cho phan hoi server. */
  pending: boolean;
  acquire: () => Promise<{ ok: boolean; status: ScreenLockStatus }>;
  release: () => Promise<void>;
}

const unlocked = (screenKey: string): ScreenLockStatus => ({
  screenKey,
  locked: false,
  lockedByUserId: null,
  lockedByName: null,
  lockedAt: null,
});

const HEARTBEAT_MS = 20_000;

/**
 * Khoa 1 MAN HINH (khong phai tung ban ghi) trong luc dang Them/Sua - dung chung cho toan bo
 * man hinh CRUD cua moi module. `screenKey` nen trung voi `CrudTable.tableId` cua man hinh do.
 * Goi acquire() TRUOC khi mo modal Them/Sua, release() khi dong modal (Huy hoac Luu xong).
 */
export function useScreenLock(
  screenKey: string | undefined,
  httpClient: AxiosInstance | undefined,
  currentUserId: string | undefined,
): UseScreenLockResult {
  const [status, setStatus] = useState<ScreenLockStatus>(() => unlocked(screenKey ?? ""));
  const [pending, setPending] = useState(false);
  const heartbeatRef = useRef<number | null>(null);
  const holdingRef = useRef(false);

  const stopHeartbeat = useCallback(() => {
    if (heartbeatRef.current != null) {
      window.clearInterval(heartbeatRef.current);
      heartbeatRef.current = null;
    }
    holdingRef.current = false;
  }, []);

  useEffect(() => {
    if (!screenKey || !httpClient) return;
    setStatus(unlocked(screenKey));

    httpClient
      .get<ApiResponse<ScreenLockStatus>>(`/api/screen-lock/${encodeURIComponent(screenKey)}`)
      .then((res) => setStatus(res.data.data))
      .catch(() => {
        // giu trang thai "unlocked" mac dinh neu khong lay duoc - khong chan man hinh vi loi mang tam thoi
      });

    const unsubscribe = subscribeTopic(`/topic/screen-lock.${screenKey}`, (body) => {
      setStatus(body as ScreenLockStatus);
    });

    return () => {
      unsubscribe();
      stopHeartbeat();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [screenKey]);

  const acquire = useCallback(async (): Promise<{ ok: boolean; status: ScreenLockStatus }> => {
    if (!screenKey || !httpClient) return { ok: true, status: unlocked("") };
    setPending(true);
    try {
      const res = await httpClient.post<ApiResponse<ScreenLockStatus>>(`/api/screen-lock/${encodeURIComponent(screenKey)}/acquire`);
      setStatus(res.data.data);
      holdingRef.current = true;
      stopHeartbeat();
      heartbeatRef.current = window.setInterval(() => {
        httpClient
          .post<ApiResponse<ScreenLockStatus>>(`/api/screen-lock/${encodeURIComponent(screenKey)}/heartbeat`)
          .then((r) => {
            setStatus(r.data.data);
            if (!r.data.data.locked || r.data.data.lockedByUserId !== currentUserId) stopHeartbeat();
          })
          .catch(() => {
            // mat mang tam thoi - lan heartbeat sau se thu lai, khong chu dong huy khoa
          });
      }, HEARTBEAT_MS);
      return { ok: true, status: res.data.data };
    } catch (err) {
      const status =
        (err as { response?: { data?: ApiResponse<ScreenLockStatus> } }).response?.data?.data ?? unlocked(screenKey);
      setStatus(status);
      return { ok: false, status };
    } finally {
      setPending(false);
    }
  }, [screenKey, httpClient, currentUserId, stopHeartbeat]);

  const release = useCallback(async (): Promise<void> => {
    stopHeartbeat();
    if (!screenKey || !httpClient) return;
    try {
      await httpClient.post(`/api/screen-lock/${encodeURIComponent(screenKey)}/release`);
    } catch {
      // khoa se tu het han sau STALE_SECONDS phia server neu request nay that bai
    }
  }, [screenKey, httpClient, stopHeartbeat]);

  return {
    status,
    isMine: status.locked && !!currentUserId && status.lockedByUserId === currentUserId,
    pending,
    acquire,
    release,
  };
}
