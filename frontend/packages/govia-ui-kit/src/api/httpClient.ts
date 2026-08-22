import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from "axios";

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

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

const TOKEN_STORAGE_KEY = "govia.tokens";

export function getStoredTokens(): AuthTokens | null {
  const raw = localStorage.getItem(TOKEN_STORAGE_KEY);
  return raw ? (JSON.parse(raw) as AuthTokens) : null;
}

export function storeTokens(tokens: AuthTokens): void {
  localStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify(tokens));
}

export function clearTokens(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
}

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
