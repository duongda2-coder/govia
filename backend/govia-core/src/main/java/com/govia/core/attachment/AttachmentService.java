package com.govia.core.attachment;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DLL attachment dung chung - moi module goi cac ham nay thay vi tu viet upload/download.
 */
public interface AttachmentService {

    Attachment upload(String entityName, UUID entityId, MultipartFile file);

    List<Attachment> listByEntity(String entityName, UUID entityId);

    /** So luong file dinh kem theo tung entityId - man hinh danh sach dung de hien badge "X file" tren moi dong, tranh N+1 goi listByEntity. */
    Map<UUID, Long> countByEntity(String entityName, List<UUID> entityIds);

    Resource loadAsResource(UUID attachmentId);

    Attachment getMetadata(UUID attachmentId);

    void delete(UUID attachmentId);
}
