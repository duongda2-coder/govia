package com.govia.core.screenlock;

import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.web.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint dung chung cho TAT CA man hinh CRUD cua moi module (Audit, People, Workflow...) -
 * khong module nao tu lam rieng co che khoa man hinh. Xem useScreenLock (frontend, @govia/ui-kit)
 * la noi goi cac endpoint nay. */
@RestController
@RequestMapping("/api/screen-lock")
public class ScreenLockController {

    private final ScreenLockService service;

    public ScreenLockController(ScreenLockService service) {
        this.service = service;
    }

    @GetMapping("/{screenKey}")
    public ApiResponse<ScreenLockStatusResponse> status(@PathVariable String screenKey,
                                                          @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.getStatus(principal.tenantId(), screenKey));
    }

    @PostMapping("/{screenKey}/acquire")
    public ResponseEntity<ApiResponse<ScreenLockStatusResponse>> acquire(@PathVariable String screenKey,
                                                                          @AuthenticationPrincipal CurrentUserPrincipal principal) {
        ScreenLockService.AcquireResult result = service.acquire(principal.tenantId(), screenKey, principal.userId(), principal.username());
        if (!result.acquired()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.ok(result.status()));
        }
        return ResponseEntity.ok(ApiResponse.ok(result.status()));
    }

    @PostMapping("/{screenKey}/heartbeat")
    public ApiResponse<ScreenLockStatusResponse> heartbeat(@PathVariable String screenKey,
                                                            @AuthenticationPrincipal CurrentUserPrincipal principal) {
        return ApiResponse.ok(service.heartbeat(principal.tenantId(), screenKey, principal.userId()));
    }

    @PostMapping("/{screenKey}/release")
    public ApiResponse<Void> release(@PathVariable String screenKey, @AuthenticationPrincipal CurrentUserPrincipal principal) {
        service.release(principal.tenantId(), screenKey, principal.userId());
        return ApiResponse.ok(null);
    }
}
