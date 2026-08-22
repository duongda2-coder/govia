package com.govia.core.attachment;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * DLL attachment dung chung - moi module goi 3 ham nay thay vi tu viet upload/download.
 */
public interface AttachmentService {

    Attachment upload(String entityName, UUID entityId, MultipartFile file);

    List<Attachment> listByEntity(String entityName, UUID entityId);

    Resource loadAsResource(UUID attachmentId);

    Attachment getMetadata(UUID attachmentId);

    void delete(UUID attachmentId);
}
