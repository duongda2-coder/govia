package com.govia.core.attachment;

import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Impl dev/on-prem don gian: luu file tren local disk, path phan theo tenant/entity.
 * Doi sang S3/MinIO sau nay chi can viet 1 impl khac cua AttachmentService,
 * khong anh huong controller/frontend dang goi DLL nay.
 */
@Service
public class LocalFileAttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository repository;
    private final AttachmentStorageProperties properties;

    public LocalFileAttachmentServiceImpl(AttachmentRepository repository, AttachmentStorageProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public Attachment upload(String entityName, UUID entityId, MultipartFile file) {
        try {
            UUID tenantId = TenantContext.getTenantId();
            Path dir = Path.of(properties.getRootPath(), tenantId.toString(), entityName, entityId.toString());
            Files.createDirectories(dir);

            String storedName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path target = dir.resolve(storedName);
            file.transferTo(target);

            Attachment attachment = new Attachment();
            attachment.setTenantId(tenantId);
            attachment.setEntityName(entityName);
            attachment.setEntityId(entityId);
            attachment.setFileName(file.getOriginalFilename());
            attachment.setContentType(file.getContentType());
            attachment.setSizeBytes(file.getSize());
            attachment.setStoragePath(target.toString());
            return repository.save(attachment);
        } catch (IOException e) {
            throw new UncheckedIOException("Khong the luu attachment", e);
        }
    }

    @Override
    public List<Attachment> listByEntity(String entityName, UUID entityId) {
        return repository.findByEntityNameAndEntityId(entityName, entityId);
    }

    @Override
    public Map<UUID, Long> countByEntity(String entityName, List<UUID> entityIds) {
        if (entityIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        for (AttachmentRepository.EntityAttachmentCount row : repository.countByEntityNameAndEntityIdIn(entityName, entityIds)) {
            counts.put(row.getEntityId(), row.getTotal());
        }
        return counts;
    }

    @Override
    public Resource loadAsResource(UUID attachmentId) {
        Attachment attachment = getMetadata(attachmentId);
        try {
            return new UrlResource(Path.of(attachment.getStoragePath()).toUri());
        } catch (MalformedURLException e) {
            throw new BusinessException("ATTACHMENT_NOT_FOUND", "Khong doc duoc file", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public Attachment getMetadata(UUID attachmentId) {
        return repository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException("ATTACHMENT_NOT_FOUND", "Attachment khong ton tai", HttpStatus.NOT_FOUND));
    }

    @Override
    public void delete(UUID attachmentId) {
        Attachment attachment = getMetadata(attachmentId);
        try {
            Files.deleteIfExists(Path.of(attachment.getStoragePath()));
        } catch (IOException e) {
            throw new UncheckedIOException("Khong the xoa file", e);
        }
        repository.delete(attachment);
    }
}
