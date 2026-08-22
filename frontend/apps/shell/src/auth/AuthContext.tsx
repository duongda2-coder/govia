import { createContext, useContext, useState, type ReactNode } from "react";
import { getStoredTokens, clearTokens } from "@govia/ui-kit";

interface CurrentUser {
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

function readStoredUser(): CurrentUser | null {
  const raw = localStorage.getItem(USER_STORAGE_KEY);
  return raw ? (JSON.parse(raw) as CurrentUser) : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUserState] = useState<CurrentUser | null>(() => (getStoredTokens() ? readStoredUser() : null));

  const setUser = (newUser: CurrentUser) => {
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(newUser));
    setUserState(newUser);
  };

  const logout = () => {
    clearTokens();
    localStorage.removeItem(USER_STORAGE_KEY);
    setUserState(null);
  };

  const hasPermission = (permissionCode: string) => user?.permissions?.includes(permissionCode) ?? false;

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
