package com.govia.identity.maildemo;

import com.govia.core.web.ApiResponse;
import com.govia.identity.maildemo.EmailDemoDtos.LogResponse;
import com.govia.identity.maildemo.EmailDemoDtos.SendRequest;
import com.govia.identity.maildemo.EmailDemoDtos.SettingRequest;
import com.govia.identity.maildemo.EmailDemoDtos.SettingResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Man hinh "Demo gui email" (Quan tri he thong) - chi SUPER_ADMIN, giong quy uoc ActivityLogController. */
@RestController
@RequestMapping("/api/admin/email-demo")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class EmailDemoController {

    private final EmailDemoService service;

    public EmailDemoController(EmailDemoService service) {
        this.service = service;
    }

    @GetMapping("/setting")
    public ApiResponse<SettingResponse> getSetting() {
        return ApiResponse.ok(service.getSetting());
    }

    @PutMapping("/setting")
    public ApiResponse<SettingResponse> saveSetting(@Valid @RequestBody SettingRequest request) {
        return ApiResponse.ok(service.saveSetting(request));
    }

    @PostMapping("/setting/test")
    public ApiResponse<Void> testConnection() {
        service.testConnection();
        return ApiResponse.ok(null);
    }

    @PostMapping("/send")
    public ApiResponse<LogResponse> send(@Valid @RequestBody SendRequest request) {
        return ApiResponse.ok(service.send(request));
    }

    @GetMapping("/logs")
    public ApiResponse<List<LogResponse>> logs() {
        return ApiResponse.ok(service.listLogs());
    }
}
