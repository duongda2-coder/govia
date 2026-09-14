package com.govia.audit.khkt.scale.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Danh muc "Quy mo tin dung va huy dong von cua chi nhanh" (sheet ZTC_KHKT_QM, bang ZTB_KHKT_QM) -
 * dung de module Ke hoach kiem toan (KHKT) tra Quy mo tin dung/huy dong von cua 1 don vi theo
 * nguong Du no noi bang/Nguon von, so sanh theo thu tu sort_order tang dan. */
@Getter
@Setter
@Entity
@Table(name = "audit_khkt_scale")
public class AuditKhktScale extends BaseEntity {

    /** Thu tu muc quy mo (STT) - dung de xac dinh muc quy mo theo nguong tang dan. */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    /** Nguong Du no noi bang (Tin dung) cua muc quy mo nay. */
    @Column(name = "credit_threshold", precision = 20, scale = 2)
    private BigDecimal creditThreshold;

    /** Nguong Nguon von (Huy dong von) cua muc quy mo nay. */
    @Column(name = "funding_threshold", precision = 20, scale = 2)
    private BigDecimal fundingThreshold;

    @Column(name = "scale_name", nullable = false, length = 100)
    private String scaleName;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
