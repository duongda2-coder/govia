package com.govia.audit.khkt.bp.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** "De xuat LVKT" - 1 dong ung voi 1 mang nghiep vu (BUSINESS_SEGMENT) duoc phong NV tick chon cho
 * 1 dong AuditKhktBpCandidate (quan he nhieu-nhieu, sheet ZTC_KHKT_BP cac cot "De xuat LVKT: xxx"). */
@Getter
@Setter
@Entity
@Table(name = "audit_khkt_bp_candidate_segment")
public class AuditKhktBpCandidateSegment extends BaseEntity {

    @Column(name = "candidate_id", nullable = false, columnDefinition = "uuid")
    private UUID candidateId;

    @Column(name = "business_segment_id", nullable = false, columnDefinition = "uuid")
    private UUID businessSegmentId;
}
