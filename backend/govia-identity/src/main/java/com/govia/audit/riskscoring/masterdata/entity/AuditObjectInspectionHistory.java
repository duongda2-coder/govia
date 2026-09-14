package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** "Lich su KT" cua 1 Doi tuong kiem toan - Don vi (sheet ZTC_DTKT1, nut "Lich su KT"): moi dong
 * la 1 lan bi/duoc thanh tra/giam sat/kiem toan trong 1 nam, theo 1 trong 6 loai hinh
 * (AuditInspectionType). Module Ke hoach kiem toan (KHKT) dung bang nay de tinh cac cot "T-5..T-1"
 * cua BP/BP2/TH/TH2 (xem sheet ZTC_KHKT_BP). */
@Getter
@Setter
@Entity
@Table(name = "risk_score_audit_object_inspection_history")
public class AuditObjectInspectionHistory extends BaseEntity {

    @Column(name = "audit_object_unit_id", nullable = false, columnDefinition = "uuid")
    private UUID auditObjectUnitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "inspection_type", nullable = false, length = 20)
    private AuditInspectionType inspectionType;

    @Column(name = "inspection_year", nullable = false)
    private Integer year;

    @Column(name = "note", length = 500)
    private String note;
}
