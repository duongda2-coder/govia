package com.govia.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Tenant KHONG ke thua BaseEntity vi ban than no la goc phan vung du lieu
 * (khong co tenantId cua rieng no). Moi entity nghiep vu khac deu tro ve day qua tenantId.
 */
@Getter
@Setter
@Entity
@Table(name = "tenant")
public class Tenant {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
