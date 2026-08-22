package com.govia.core.audit;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Nhat ky thao tac dung chung TOAN PLATFORM (polymorphic, giong Attachment).
 * Ghi nhan CREATE/UPDATE/DELETE tren bat ky entity nao cua bat ky module nao,
 * lam nen tang cho GOVIA Audit va cho truy vet compliance.
 * createdBy/createdAt (ke thua tu BaseEntity) chinh la nguoi thuc hien & thoi diem.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_log")
public class AuditLog extends BaseEntity {

    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    @Column(name = "entity_id", columnDefinition = "uuid")
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private AuditAction action;

    /**
     * JSON mo ta thay doi (before/after) hoac ghi chu ngu canh.
     * Dung VARCHAR(4000) tuong minh (khong @Lob) de cot vat ly GIONG HET nhau tren moi DB
     * (Postgres/H2/Oracle) - CLOB truu tuong cua Liquibase tao ra kieu cot khac nhau tung noi,
     * khien Hibernate schema-validation luon lech tren mot trong cac DB.
     */
    @Column(name = "detail", length = 4000)
    private String detail;

    @Column(name = "source_ip", length = 64)
    private String sourceIp;
}
