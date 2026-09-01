package com.govia.identity.dto;

import java.util.List;

/**
 * Ket qua cua {@code POST /api/auth/login}. Luon tra HTTP 200 - frontend phan biet 2 nhanh qua
 * "status", tranh phai xu ly them 1 ma loi rieng cho truong hop "chua sai gi ca, chi la dang co
 * phien khac dang hoat dong".
 *  - SUCCESS: dang nhap thanh cong nhu binh thuong, "login" khong null.
 *  - CONFLICT: dung mat khau nhung tai khoan dang co phien ACTIVE o noi khac - "sessions" +
 *    "pendingToken" duoc tra ve de FE hoi nguoi dung "da phien cu" hay "dang nhap song song" roi
 *    goi tiep {@code POST /api/auth/login/resolve}.
 */
public record LoginOutcome(
        String status,
        LoginResponse login,
        List<ActiveSessionInfo> sessions,
        String pendingToken
) {

    public static LoginOutcome success(LoginResponse login) {
        return new LoginOutcome("SUCCESS", login, null, null);
    }

    public static LoginOutcome conflict(List<ActiveSessionInfo> sessions, String pendingToken) {
        return new LoginOutcome("CONFLICT", null, sessions, pendingToken);
    }
}
