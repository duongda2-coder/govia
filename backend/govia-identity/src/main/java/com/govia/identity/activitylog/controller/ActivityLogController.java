package com.govia.identity.activitylog.controller;

import com.govia.core.web.ApiResponse;
import com.govia.identity.activitylog.dto.ActivityLogFilter;
import com.govia.identity.activitylog.dto.ActivityLogResponse;
import com.govia.identity.activitylog.service.ActivityLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Man hinh "Nhat ky thao tac" - chi SUPER_ADMIN xem duoc, giong quy uoc cua RoleController/
 * AccountController (khong dung permission code rieng, chi role-based). */
@RestController
@RequestMapping("/api/admin/activity-log")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class ActivityLogController {

    private final ActivityLogService service;

    public ActivityLogController(ActivityLogService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Page<ActivityLogResponse>> list(@ModelAttribute ActivityLogFilter filter, Pageable pageable) {
        return ApiResponse.ok(service.list(filter, pageable));
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(@ModelAttribute ActivityLogFilter filter) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"activity_log.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportExcel(filter));
    }

    @GetMapping("/export/word")
    public ResponseEntity<byte[]> exportWord(@ModelAttribute ActivityLogFilter filter) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"activity_log.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(service.exportWord(filter));
    }
}
