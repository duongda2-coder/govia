package com.govia.audit.tdkp.unitrec;

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
import java.util.UUID;

/** Kiến nghị của Đơn vị/Bộ phận đối với KTNB (sheet 6 ZTC_TDKP_KTNB). */
@Getter
@Setter
@Entity
@Table(name = "audit_tdkp_unit_recommendation")
public class AuditTdkpUnitRecommendation extends BaseEntity {

    /** "Mã Kiến nghị KTNB" - hệ thống tự sinh (KNIA001...). */
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "report_number", length = 20)
    private String reportNumber;

    @Column(name = "report_date")
    private LocalDate reportDate;

    /** "Đơn vị kiến nghị" - chọn trong đối tượng kiểm toán (ztc_dt_kt1, ztc_dt_kt2). */
    @Column(name = "unit_id", columnDefinition = "uuid")
    private UUID unitId;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "deadline")
    private LocalDate deadline;

    /** "Tình hình thực hiện kiến nghị". */
    @Column(name = "implementation", length = 500)
    private String implementation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private TdkpStatus status;

    /** "Đánh giá tình hình thực hiện kiến nghị". */
    @Column(name = "evaluation", length = 500)
    private String evaluation;

    @Column(name = "note", length = 500)
    private String note;
}
