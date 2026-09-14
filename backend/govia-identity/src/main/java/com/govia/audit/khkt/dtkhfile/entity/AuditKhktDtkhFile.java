package com.govia.audit.khkt.dtkhfile.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** "Báo cáo tham khảo từ các bộ phận cho PKH lập dự thảo" (sheet ZTC_DTKH_FILE, bang ZTB_DTKH_FILE) -
 * moi dong = 1 file dinh kem (dung dac ta "mỗi file một dòng"). File vat ly luu qua AttachmentService
 * dung chung (entityName=ENTITY_NAME, entityId = id cua dong nay), khong luu duong dan o day. Cot
 * "User" cua sheet lay thang tu createdBy (BaseEntity) - khong can truong rieng. */
@Getter
@Setter
@Entity
@Table(name = "audit_khkt_dtkh_file")
public class AuditKhktDtkhFile extends BaseEntity {

    public static final String ATTACHMENT_ENTITY_NAME = "AUDIT_KHKT_DTKH_FILE";

    @Column(name = "khkt_year", nullable = false)
    private Integer year;

    @Column(name = "department_id", nullable = false, columnDefinition = "uuid")
    private UUID departmentId;

    @Column(name = "version_id", columnDefinition = "uuid")
    private UUID versionId;

    @Column(name = "note", length = 250)
    private String note;
}
