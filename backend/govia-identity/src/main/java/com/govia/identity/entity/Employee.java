package com.govia.identity.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Nhan vien - la "danh tinh goc" ma tat ca module tham chieu toi (employeeCode).
 * UserAccount la tai khoan dang nhap gan voi 1 Employee (co the null cho tai khoan he thong/tich hop).
 */
@Getter
@Setter
@Entity
@Table(name = "employee")
public class Employee extends BaseEntity {

    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "email", length = 255)
    private String email;

    /** Email ca nhan, tach biet voi email cong ty/dang nhap o tren. */
    @Column(name = "personal_email", length = 255)
    private String personalEmail;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "org_unit_id", columnDefinition = "uuid")
    private UUID orgUnitId;

    /** Chi doc, dung de join khi loc/sort theo ten don vi (vd "orgUnit.name") - ghi du lieu van qua orgUnitId o tren. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_unit_id", insertable = false, updatable = false)
    private OrganizationUnit orgUnit;

    @Column(name = "position_id", columnDefinition = "uuid")
    private UUID positionId;

    /** Chi doc, dung de join khi loc/sort theo ten chuc danh (vd "position.name") - ghi du lieu van qua positionId o tren. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", insertable = false, updatable = false)
    private Position position;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "id_number", length = 30)
    private String idNumber;

    @Column(name = "manager_id", columnDefinition = "uuid")
    private UUID managerId;

    /** Cap bac nhan su (N1..N6) - dung lam nguong dung dây phe duyet dong, xem EmployeeRankLevel. */
    @Enumerated(EnumType.STRING)
    @Column(name = "rank_level", length = 10)
    private EmployeeRankLevel rankLevel;

    /** Chi doc, dung de join khi loc/sort theo ten quan ly (vd "manager.fullName") - ghi du lieu van qua managerId o tren. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", insertable = false, updatable = false)
    private Employee manager;

    // ===================== Bo sung ho so KTV (sheet ZTC_NV) =====================

    @Column(name = "ethnicity", length = 50)
    private String ethnicity;

    @Column(name = "hometown", length = 255)
    private String hometown;

    @Column(name = "party_join_date")
    private LocalDate partyJoinDate;

    /** Ngay vao Ban Kiem Soat/Kiem toan noi bo - khac voi hireDate (ngay vao Agribank noi chung). */
    @Column(name = "audit_dept_join_date")
    private LocalDate auditDeptJoinDate;

    @Column(name = "prior_work_history", length = 1000)
    private String priorWorkHistory;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_level", length = 20)
    private EmployeeEducationLevel educationLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "political_level", length = 20)
    private EmployeePoliticalLevel politicalLevel;

    @Column(name = "foreign_language_level", length = 20)
    private String foreignLanguageLevel;

    @Column(name = "it_skill_level", length = 20)
    private String itSkillLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "auditor_classification", length = 20)
    private EmployeeAuditorClassification auditorClassification;

    @Column(name = "team_lead_capable", nullable = false)
    private boolean teamLeadCapable = false;

    /** Danh sach ten chi nhanh da tham gia kiem toan nam truoc, phan cach bang "; " - chon nhieu tu danh muc
     * Doi tuong kiem toan (AuditObjectUnit, unitType=CN) ben frontend, luu lai duoi dang text don gian. */
    @Column(name = "audited_branches", length = 500)
    private String auditedBranches;

    @Column(name = "other_duties", length = 255)
    private String otherDuties;

    /** Danh sach ten chi nhanh co nguoi lien quan, phan cach bang "; " - cung quy uoc voi auditedBranches. */
    @Column(name = "related_person_branches", length = 500)
    private String relatedPersonBranches;

    @Column(name = "on_leave", nullable = false)
    private boolean onLeave = false;

    /** "Linh vuc" - link toi AuditMasterDataItem danh muc BUSINESS_SEGMENT (sheet ZTC_Mang_NV). */
    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;
}
