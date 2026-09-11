package com.govia.audit.planengagement.processengagement.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** "Cuoc kiem toan theo quy trinh/nghiep vu" (CKT quy trinh) - man hinh "Tao CKT quy trinh": thay
 * vi chon "Doi tuong kiem toan" (don vi/chi nhanh), CKT quy trinh gan truc tiep voi 1 "Nghiep vu"
 * (BUSINESS_SEGMENT) - nguon: file "Tao CKT (3).xlsx", sheet "man hinh tao CKT quy trinh". Tu
 * "Tao CKT (4).xlsx" sheet "QL CKT quy trinh": day la CHA cua nhieu {@link
 * com.govia.audit.planengagement.entity.AuditEngagement} (CKT con, 1 cai/chi nhanh di kiem toan)
 * lien ket qua AuditEngagement.processEngagementId. */
@Getter
@Setter
@Entity
@Table(name = "audit_process_engagement")
public class AuditProcessEngagement extends BaseEntity {

    /** Ma CKT - he thong tu sinh khi luu, khong nhan tu client. Quy tac: "QT" (Quy trinh) + Ma nghiep
     * vu (code cua AuditMasterDataItem BUSINESS_SEGMENT duoc chon) + Nam + STT 2 chu so (dem theo
     * nghiep vu + nam) - xem sheet "man hinh tao CKT quy trinh": "Ma QT/HD + nghiep vu + Nam + STT". */
    @Column(name = "code", nullable = false, length = 30, unique = true)
    private String code;

    /** "QT" (Quy trinh) hoac "HD" (Hoat dong) - tien to cua ma CKT, xem sheet "QL CKT quy trinh"
     * cua "Tao CKT (4).xlsx": "co 2 loai doi tuong QT la quy trinh, HD la Hoat dong". */
    @Column(name = "object_type", nullable = false, length = 2)
    private String objectType = "QT";

    /** Nghiep vu kiem toan - AuditMasterDataItem thuoc danh muc BUSINESS_SEGMENT. */
    @Column(name = "business_segment_id", nullable = false, columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "engagement_year", nullable = false)
    private Integer year;

    @Column(name = "expected_month", nullable = false)
    private Integer expectedMonth;

    @Column(name = "decision_date", nullable = false)
    private LocalDate decisionDate;

    @Column(name = "team_lead_employee_id", nullable = false, columnDefinition = "uuid")
    private UUID teamLeadEmployeeId;

    @Column(name = "decision_number", nullable = false, length = 50)
    private String decisionNumber;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "work_set_code", length = 50)
    private String workSetCode;
}
