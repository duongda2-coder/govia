package com.govia.audit.khkt.th.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** "TH de xuat LVKT" - linh vuc Phong Ke hoach QUYET DINH theo doi (khac voi "BP de xuat LVKT" la
 * tong hop CHI DE THAM KHAO tu cac phong, tinh dong o AuditKhktThService, khong luu bang nay). */
@Getter
@Setter
@Entity
@Table(name = "audit_khkt_th_candidate_segment")
public class AuditKhktThCandidateSegment extends BaseEntity {

    @Column(name = "candidate_id", nullable = false, columnDefinition = "uuid")
    private UUID candidateId;

    @Column(name = "business_segment_id", nullable = false, columnDefinition = "uuid")
    private UUID businessSegmentId;
}
