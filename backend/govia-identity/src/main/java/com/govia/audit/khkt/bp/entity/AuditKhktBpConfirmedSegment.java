package com.govia.audit.khkt.bp.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Ban sao cua AuditKhktBpCandidateSegment sau khi xac nhan sang Phan 2 - xem AuditKhktBpConfirmed. */
@Getter
@Setter
@Entity
@Table(name = "audit_khkt_bp_confirmed_segment")
public class AuditKhktBpConfirmedSegment extends BaseEntity {

    @Column(name = "confirmed_id", nullable = false, columnDefinition = "uuid")
    private UUID confirmedId;

    @Column(name = "business_segment_id", nullable = false, columnDefinition = "uuid")
    private UUID businessSegmentId;
}
