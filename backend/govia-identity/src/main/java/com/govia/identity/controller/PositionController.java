package com.govia.identity.controller;

import com.govia.core.export.ImportResult;
import com.govia.core.web.ApiResponse;
import com.govia.identity.dto.PositionActiveRequest;
import com.govia.identity.dto.PositionRequest;
import com.govia.identity.dto.PositionResponse;
import com.govia.identity.service.PositionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/positions")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_PEOPLE.POSITION.VIEW')")
    public ApiResponse<List<PositionResponse>> list() {
        return ApiResponse.ok(positionService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.POSITION.VIEW')")
    public ApiResponse<PositionResponse> getById(@PathVariable UUID id) {
        return ApiResponse.ok(positionService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PEOPLE.POSITION.CREATE')")
    public ApiResponse<PositionResponse> create(@Valid @RequestBody PositionRequest request) {
        return ApiResponse.ok(positionService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.POSITION.EDIT')")
    public ApiResponse<PositionResponse> update(@PathVariable UUID id, @Valid @RequestBody PositionRequest request) {
        return ApiResponse.ok(positionService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.POSITION.EDIT')")
    public ApiResponse<PositionResponse> setActive(@PathVariable UUID id, @Valid @RequestBody PositionActiveRequest request) {
        return ApiResponse.ok(positionService.setActive(id, request.active()));
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.POSITION.EXPORT')")
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"positions.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(positionService.exportExcel());
    }

    @GetMapping("/export/word")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.POSITION.EXPORT')")
    public ResponseEntity<byte[]> exportWord() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"positions.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(positionService.exportWord());
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('PERM_PEOPLE.POSITION.IMPORT')")
    public ApiResponse<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(positionService.importFromExcel(file));
    }
}
