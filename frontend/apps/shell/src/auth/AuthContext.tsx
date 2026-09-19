import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { getStoredTokens, clearTokens, clearLegacySharedSession, initSocket, disconnectSocket, subscribeTopic } from "@govia/ui-kit";
import { httpClient, API_BASE_URL } from "../api/client";

interface CurrentUser {
  userId: string;
  username: string;
  employeeCode: string | null;
  tenantId: string;
  roles: string[];
  permissions: string[];
}

interface AuthContextValue {
  user: CurrentUser | null;
  isAuthenticated: boolean;
  setUser: (user: CurrentUser) => void;
  logout: () => void;
  /** true neu user co quyen truyen vao (hoac la SUPER_ADMIN - da duoc BE mo rong toan bo quyen vao permissions). */
  hasPermission: (permissionCode: string) => boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const USER_STORAGE_KEY = "govia.user";
// Cung sessionStorage (rieng tung tab) nhu token - xem ghi chu o httpClient.ts.
clearLegacySharedSession(USER_STORAGE_KEY);

function readStoredUser(): CurrentUser | null {
  const raw = sessionStorage.getItem(USER_STORAGE_KEY);
  return raw ? (JSON.parse(raw) as CurrentUser) : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUserState] = useState<CurrentUser | null>(() => (getStoredTokens() ? readStoredUser() : null));

  const setUser = (newUser: CurrentUser) => {
    sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(newUser));
    setUserState(newUser);
  };

  const logout = () => {
    httpClient.post("/api/auth/logout").catch(() => {
      // token co the da het han/bi thu hoi san - khong sao, van xoa phien phia client binh thuong
    });
    disconnectSocket();
    clearTokens();
    sessionStorage.removeItem(USER_STORAGE_KEY);
    setUserState(null);
  };

  const hasPermission = (permissionCode: string) => user?.permissions?.includes(permissionCode) ?? false;

  /* Mo 1 ket noi WebSocket DUY NHAT cho ca app khi con dang nhap - dung de nhan thong bao "bi da
   * ra" tuc thi (khac voi phat hien qua 401 tren request REST tiep theo, co the mat vai phut neu
   * nguoi dung dang khong tuong tac gi). */
  useEffect(() => {
    if (!user) return;
    initSocket(API_BASE_URL);
    const unsubscribe = subscribeTopic("/user/queue/session-kicked", () => {
      disconnectSocket();
      clearTokens();
      sessionStorage.removeItem(USER_STORAGE_KEY);
      setUserState(null);
      window.location.href = "/login?reason=kicked";
    });
    return unsubscribe;
  }, [user]);

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, setUser, logout, hasPermission }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth phai duoc dung ben trong AuthProvider");
  return ctx;
}
