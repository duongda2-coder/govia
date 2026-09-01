package com.govia.identity.controller;

import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import com.govia.identity.dto.ChangePasswordRequest;
import com.govia.identity.dto.LoginOutcome;
import com.govia.identity.dto.LoginRequest;
import com.govia.identity.dto.LoginResolveRequest;
import com.govia.identity.dto.LoginResponse;
import com.govia.identity.dto.RefreshRequest;
import com.govia.identity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diem xac thuc DUY NHAT cua toan platform. Moi module khac khong tu lam login,
 * chi verify JWT do day phat hanh (xem JwtAuthenticationFilter trong govia-core).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginOutcome> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.login(request, deviceInfo(httpRequest), clientIp(httpRequest)));
    }

    /** Buoc 2 sau khi /login tra ve CONFLICT - nguoi dung da chon "da phien cu" hay "dang nhap
     * song song". Cung la endpoint public (xem SecurityConfig) vi luc nay chua co token. */
    @PostMapping("/login/resolve")
    public ApiResponse<LoginResponse> resolveLogin(@Valid @RequestBody LoginResolveRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.resolveLogin(request, deviceInfo(httpRequest), clientIp(httpRequest)));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    /** Dang xuat phien HIEN TAI (khac logout "toan bo thiet bi" - xem login/resolve KICK_OTHERS). */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        authService.logout(principal.sessionJti());
        return ApiResponse.ok(null);
    }

    /** Doi mat khau CHINH MINH - can dang nhap (khac /login, /refresh la public). */
    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                             @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.userId(), request);
        return ApiResponse.ok(null);
    }

    private String deviceInfo(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua == null ? null : ua.substring(0, Math.min(ua.length(), 255));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
