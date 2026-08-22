package com.govia.core.attachment;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Bang attachment dung chung (polymorphic) cho TAT CA module: Audit evidence,
 * Risk document, Vendor contract, Employee ho so... chi can truyen entityName + entityId,
 * khong module nao can tu tao bang attachment rieng.
 */
@Getter
@Setter
@Entity
@Table(name = "attachment")
public class Attachment extends BaseEntity {

    /** Ten entity nghiep vu, vd: "AUDIT_FINDING", "EMPLOYEE", "VENDOR_CONTRACT" */
    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    @Column(name = "entity_id", nullable = false, columnDefinition = "uuid")
    private UUID entityId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    /** Duong dan luu tru vat ly (local disk hom nay, S3/MinIO key sau nay). */
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;
}
