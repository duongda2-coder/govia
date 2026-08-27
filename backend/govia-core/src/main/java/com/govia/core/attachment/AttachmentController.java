package com.govia.core.attachment;

import com.govia.core.web.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoint attachment DUY NHAT dung chung cho toan platform.
 * Man hinh cua bat ky module nao (Audit finding, Vendor contract, Employee ho so...)
 * chi can goi /api/attachments?entityName=XXX&entityId=YYY - khong tu viet controller rieng.
 */
@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping
    public ApiResponse<Attachment> upload(@RequestParam String entityName,
                                           @RequestParam UUID entityId,
                                           @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(attachmentService.upload(entityName, entityId, file));
    }

    @GetMapping
    public ApiResponse<List<Attachment>> list(@RequestParam String entityName, @RequestParam UUID entityId) {
        return ApiResponse.ok(attachmentService.listByEntity(entityName, entityId));
    }

    /** So luong file dinh kem theo tung entityId - man hinh danh sach goi 1 lan cho ca trang thay vi N+1 goi /api/attachments. */
    @GetMapping("/counts")
    public ApiResponse<Map<UUID, Long>> counts(@RequestParam String entityName, @RequestParam List<UUID> entityIds) {
        return ApiResponse.ok(attachmentService.countByEntity(entityName, entityIds));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable("id") UUID id) {
        Attachment meta = attachmentService.getMetadata(id);
        Resource resource = attachmentService.loadAsResource(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getFileName() + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        meta.getContentType() == null ? "application/octet-stream" : meta.getContentType()))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") UUID id) {
        attachmentService.delete(id);
        return ApiResponse.ok(null);
    }
}
