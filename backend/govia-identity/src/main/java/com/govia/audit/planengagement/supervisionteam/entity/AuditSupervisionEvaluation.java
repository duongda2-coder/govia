package com.govia.audit.planengagement.supervisionteam.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** "To giam sat thuc hien danh gia" - 1 dong duy nhat cho moi (engagement, employee) trong to
 * giam sat cua CKT do; chi CHINH CHU (employeeId = nhan vien dang dang nhap) moi duoc ghi de dong
 * cua minh - xem AuditSupervisionTeamService.saveMyEvaluation(). evaluatedAt tu dong = thoi diem
 * luu gan nhat, khong nhan tu client. */
@Getter
@Setter
@Entity
@Table(name = "audit_supervision_evaluation")
public class AuditSupervisionEvaluation extends BaseEntity {

    @Column(name = "engagement_id", nullable = false, columnDefinition = "uuid")
    private UUID engagementId;

    @Column(name = "employee_id", nullable = false, columnDefinition = "uuid")
    private UUID employeeId;

    /** "Tien do". */
    @Column(name = "progress", nullable = false)
    private boolean progress;

    /** "Dam bao noi dung de cuong". */
    @Column(name = "content_assured", nullable = false)
    private boolean contentAssured;

    /** "Dam bao chat luong". */
    @Column(name = "quality_assured", nullable = false)
    private boolean qualityAssured;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;
}
