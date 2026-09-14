package com.govia.audit.riskscoring.masterdata.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Danh muc doi tuong kiem toan HO, Giam sat CC, Chi nhanh (sheet ZTC_DTKT1, bang ZTB_DTKT10). */
@Getter
@Setter
@Entity
@Table(name = "risk_score_audit_object_unit")
public class AuditObjectUnit extends BaseEntity {

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Ma tham chieu toi AuditMasterDataItem (category UNIT_TYPE) - khong con la enum cung 3 gia tri. */
    @Column(name = "unit_type", nullable = false, length = 10)
    private String unitType;

    /** "Loai doi tuong kiem toan" - link toi danh muc goc AuditObjectCategory (sheet ZTC_Loai_Dtkt). */
    @Column(name = "audit_object_category_id", columnDefinition = "uuid")
    private UUID auditObjectCategoryId;

    @Column(name = "established_date")
    private LocalDate establishedDate;

    @Column(name = "restructure_date")
    private LocalDate restructureDate;

    @Column(name = "restructure_note", length = 255)
    private String restructureNote;

    @Column(name = "total_staff")
    private Integer totalStaff;

    @Column(name = "leader_count")
    private Integer leaderCount;

    @Column(name = "staff_count")
    private Integer staffCount;

    @Column(name = "rank_value")
    private Integer rankValue;

    /** "Thuoc tuyen bao ve" - link toi RiskGroupHO (sub-module Cham Diem, sheet ZTC_Nhom_DGRR_HO). */
    @Column(name = "defense_line_group_id", columnDefinition = "uuid")
    private UUID defenseLineGroupId;

    @Column(name = "operating_regulation", length = 500)
    private String operatingRegulation;

    @Column(name = "main_function", length = 500)
    private String mainFunction;

    @Column(name = "key_findings", length = 500)
    private String keyFindings;

    /** "Ngay cap nhat thong tin" - tu dong lay ngay he thong luc luu, khong cho NSD nhap. */
    @Column(name = "info_updated_date")
    private LocalDate infoUpdatedDate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Ma tham chieu toi AuditMasterDataItem (category GEOGRAPHIC_AREA) - cung pattern voi unit_type. Dung cho module KHKT. */
    @Column(name = "geographic_area", length = 10)
    private String geographicArea;

    /** "Quy mo hoat dong cua don vi" (sheet ZTC_DTKT1) - Du no noi bang. Dung de tra Quy mo tin dung o module KHKT. */
    @Column(name = "on_balance_sheet_loan", precision = 20, scale = 2)
    private BigDecimal onBalanceSheetLoan;

    /** "Quy mo hoat dong cua don vi" (sheet ZTC_DTKT1) - Nguon von. Dung de tra Quy mo huy dong von o module KHKT. */
    @Column(name = "funding_source", precision = 20, scale = 2)
    private BigDecimal fundingSource;
}
