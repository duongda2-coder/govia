import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from "axios";
import { reconnectSocket } from "../ws/wsClient";

/**
 * Response wrapper chuan tra ve tu moi API GOVIA (xem com.govia.core.web.ApiResponse ben backend).
 */
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  errorCode: string | null;
  message: string | null;
  timestamp: string;
}

/**
 * Lay thong diep loi cu the tu backend (vd "Khong the xoa: nhan vien nay dang la quan ly truc tiep
 * cua nguoi khac" tu BusinessException) thay vi chi hien 1 thong bao chung chung nhu "Xoa that bai" -
 * dung trong moi catch block cua thao tac goi API (xoa/luu...). Tra ve fallback neu response khong
 * co message cu the (loi mang, 500 khong ro nguyen nhan...).
 */
export function getApiErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as ApiResponse<unknown> | undefined;
    if (data?.message) {
      return data.message;
    }
  }
  return fallback;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

const TOKEN_STORAGE_KEY = "govia.tokens";

/* Token luu o sessionStorage (RIENG TUNG TAB), khong phai localStorage (dung chung moi tab cua trinh
 * duyet): neu dung localStorage thi dang nhap user2 o tab 2 se ghi de token cua user1 o tab 1, khien
 * moi thao tac o tab 1 bi ghi nhan la user2. Doi lai, mo tab moi phai dang nhap lai. */
export function getStoredTokens(): AuthTokens | null {
  const raw = sessionStorage.getItem(TOKEN_STORAGE_KEY);
  return raw ? (JSON.parse(raw) as AuthTokens) : null;
}

export function storeTokens(tokens: AuthTokens): void {
  sessionStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify(tokens));
}

export function clearTokens(): void {
  sessionStorage.removeItem(TOKEN_STORAGE_KEY);
}

/* Don token cu con sot lai o localStorage tu ban truoc - neu khong se ton tai mai, va (tren may dung
 * chung) la 1 token dang nhap nam ngoai tam kiem soat cua nut Dang xuat. */
export function clearLegacySharedSession(...keys: string[]): void {
  try {
    [TOKEN_STORAGE_KEY, ...keys].forEach((key) => localStorage.removeItem(key));
  } catch {
    // localStorage bi chan - khong co gi de don
  }
}
clearLegacySharedSession();

/**
 * DLL goi API dung chung cho TOAN BO man hinh cua moi module GOVIA:
 * - Tu dong gan Bearer token
 * - Tu dong refresh token khi gap 401 (retry 1 lan)
 * - Man hinh chi can import instance nay, khong tu tao axios rieng.
 */
export function createGoviaHttpClient(baseURL: string): AxiosInstance {
  const client = axios.create({ baseURL });

  client.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    const tokens = getStoredTokens();
    if (tokens?.accessToken) {
      config.headers.set("Authorization", `Bearer ${tokens.accessToken}`);
    }
    return config;
  });

  let refreshingPromise: Promise<AuthTokens> | null = null;

  const refreshAccessToken = async (): Promise<AuthTokens> => {
    const tokens = getStoredTokens();
    if (!tokens?.refreshToken) {
      throw new Error("Khong co refresh token");
    }
    const response = await axios.post<ApiResponse<AuthTokens>>(`${baseURL}/api/auth/refresh`, {
      refreshToken: tokens.refreshToken,
    });
    const newTokens = response.data.data;
    storeTokens(newTokens);
    reconnectSocket();
    return newTokens;
  };

  client.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const originalRequest = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;

      if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
        originalRequest._retry = true;
        try {
          refreshingPromise ??= refreshAccessToken().finally(() => {
            refreshingPromise = null;
          });
          const tokens = await refreshingPromise;
          originalRequest.headers.set("Authorization", `Bearer ${tokens.accessToken}`);
          return client(originalRequest);
        } catch {
          clearTokens();
          window.location.href = "/login";
        }
      }
      return Promise.reject(error);
    },
  );

  return client;
}
