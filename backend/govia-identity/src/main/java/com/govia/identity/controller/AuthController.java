package com.govia.identity.controller;

import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import com.govia.identity.dto.ChangePasswordRequest;
import com.govia.identity.dto.LoginRequest;
import com.govia.identity.dto.LoginResponse;
import com.govia.identity.dto.RefreshRequest;
import com.govia.identity.service.AuthService;
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
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    /** Doi mat khau CHINH MINH - can dang nhap (khac /login, /refresh la public). */
    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                             @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.userId(), request);
        return ApiResponse.ok(null);
    }
}
