package com.govia.audit.tdkp.unitrec;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
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

    /** "Đơn vị kiến nghị" - nhập tay tự do theo phan hoi nguoi dung (test24.9), KHONG chon tu danh
     * muc doi tuong kiem toan nhu truoc. */
    @Column(name = "unit_name", length = 255)
    private String unitName;

    /** "Đối tượng kiến nghị" - nhập tay tự do (khac voi unitId = don vi kien nghi). */
    @Column(name = "recommendation_target", length = 100)
    private String recommendationTarget;

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

    /** "Trạng thái phê duyệt" - nhap/chon tay, khong gan voi quy trinh Flowable. */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20)
    private AssignmentApprovalStatus approvalStatus;
}
