package com.govia.audit.khkt.common.entity;

/** Nguon goc doi tuong kiem toan duoc de xuat trong module Ke hoach kiem toan (KHKT): BRANCH ung
 * voi nguon ztc_dgrr_n (RiskBranchScoreExpertRank), OTHER ung voi nguon ztc_xhrr_khac_cg
 * (RiskAssessmentOtherExpertRank) - xem sheet ZTC_KHKT_BP dong "Neu nguon du lieu la tu ztc_dgrr_n
 * thi loai doi tuong la CN...". */
public enum AuditKhktSourceType {
    BRANCH,
    OTHER
}
