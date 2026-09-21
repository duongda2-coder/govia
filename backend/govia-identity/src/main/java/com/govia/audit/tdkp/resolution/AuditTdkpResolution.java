package com.govia.audit.tdkp.resolution;

import com.govia.audit.tdkp.common.TdkpStatus;
import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Theo dõi tình hình thực hiện nghị quyết của HĐTV (sheet 5 ZTC_TDKP_NQ). */
@Getter
@Setter
@Entity
@Table(name = "audit_tdkp_resolution")
public class AuditTdkpResolution extends BaseEntity {

    /** "Mã quản lý kiến nghị nghị quyết" - hệ thống tự sinh (NQ001...). */
    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "resolution_number", nullable = false, length = 20)
    private String resolutionNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "content", length = 500)
    private String content;

    /** "Tình hình thực hiện". */
    @Column(name = "implementation", length = 500)
    private String implementation;

    /** "Đánh giá (Đã/Đang/Chưa thực hiện)". */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private TdkpStatus status;

    /** "Người giám sát" - nhập tay (Char 50). */
    @Column(name = "supervisor", length = 50)
    private String supervisor;

    /** "Đánh giá về việc ban hành nghị quyết của HĐTV". */
    @Column(name = "issuance_evaluation", length = 500)
    private String issuanceEvaluation;

    @Column(name = "note", length = 500)
    private String note;
}
