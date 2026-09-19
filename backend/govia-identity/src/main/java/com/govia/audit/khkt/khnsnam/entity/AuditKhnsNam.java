package com.govia.audit.khkt.khnsnam.entity;

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

/** "Dự kiến nhân sự thực hiện kiểm toán năm, đợt" (sheet ZTC_KHNS_NAM) - 1 dong / 1 nhan vien / 1
 * nam, tao "lazy" khi NSD sua lan dau (giong AuditKhktThang - man hinh tu dong liet ke TAT CA nhan
 * vien cua nam, chi luu dong nao da duoc dien thong tin). "Chi tiet dot" (12 thang, moi thang chon
 * 1 doi tuong kiem toan tu TH2) luu truc tiep 12 cot month1..month12 thay vi bang con rieng - moi
 * nhan vien chi co dung 1 lua chon cho moi thang trong nam. */
@Getter
@Setter
@Entity
@Table(name = "audit_khns_nam")
public class AuditKhnsNam extends BaseEntity {

    @Column(name = "khkt_year", nullable = false)
    private Integer year;

    @Column(name = "employee_id", nullable = false, columnDefinition = "uuid")
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_team", length = 20)
    private AuditKhnsRoleInTeam roleInTeam;

    @Column(name = "other_duties", length = 120)
    private String otherDuties;

    @Column(name = "decision_number", length = 25)
    private String decisionNumber;

    /** Ngay quyet dinh - khong co trong dac ta goc (chi co So quyet dinh), bo sung theo yeu cau NSD
     * de ChuyenThongTinKHTH co du lieu dien AuditEngagement.decisionDate (bat buoc). */
    @Column(name = "decision_date")
    private LocalDate decisionDate;

    /** "Dự kiến thời gian" - dac ta tham chieu toi ztc_dot_kt (danh muc dot kiem toan) nhung sheet
     * do khong co trong file dac ta nay - rut gon thanh text tu do (vd "Dot 1"). */
    @Column(name = "expected_batch", length = 100)
    private String expectedBatch;

    @Column(name = "note", length = 120)
    private String note;

    /** true = can bo da duoc dua vao man hinh KHNS_NAM qua nut "Cap nhat danh sach can bo" (lay tu KHNS_PB). Phan bo thang/chuc vu
     * van doc truc tiep tu du lieu KHNS_PB; co nay chi quyet dinh can bo co nam trong danh sach KHNS_NAM hay khong. */
    @Column(name = "khns_listed", nullable = false)
    private boolean listed;

    @Column(name = "month1_audit_object_code", length = 20)
    private String month1AuditObjectCode;

    @Column(name = "month2_audit_object_code", length = 20)
    private String month2AuditObjectCode;

    @Column(name = "month3_audit_object_code", length = 20)
    private String month3AuditObjectCode;

    @Column(name = "month4_audit_object_code", length = 20)
    private String month4AuditObjectCode;

    @Column(name = "month5_audit_object_code", length = 20)
    private String month5AuditObjectCode;

    @Column(name = "month6_audit_object_code", length = 20)
    private String month6AuditObjectCode;

    @Column(name = "month7_audit_object_code", length = 20)
    private String month7AuditObjectCode;

    @Column(name = "month8_audit_object_code", length = 20)
    private String month8AuditObjectCode;

    @Column(name = "month9_audit_object_code", length = 20)
    private String month9AuditObjectCode;

    @Column(name = "month10_audit_object_code", length = 20)
    private String month10AuditObjectCode;

    @Column(name = "month11_audit_object_code", length = 20)
    private String month11AuditObjectCode;

    @Column(name = "month12_audit_object_code", length = 20)
    private String month12AuditObjectCode;
}
