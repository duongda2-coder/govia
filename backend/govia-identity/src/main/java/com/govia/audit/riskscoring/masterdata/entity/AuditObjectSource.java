package com.govia.audit.riskscoring.masterdata.entity;

/**
 * Danh muc "Doi tuong kiem toan" cu the ma 1 AuditObjectCategory ("Loai doi tuong kiem toan") tro
 * toi khi tra cuu "Ma doi tuong KT" (vd o man hinh Cham diem rui ro khac, sheet ZTC_CDRR_KHAC).
 * Truoc day quan he nay bi hard-code theo dung 3 MA co dinh (HO/CTC/KTQT), moi ma khac deu roi vao
 * PROJECT mac dinh - sai ngay khi NSD tao 1 category moi (vd "QT") ma khong dat dung ma "KTQT". Nay
 * chuyen thanh 1 truong tuong minh, NSD tu chon khi tao/sua category, khong con phu thuoc vao dung
 * chinh xac ma code nao.
 */
public enum AuditObjectSource {
    UNIT,       // ZTC_DTKT1 - AuditObjectUnit (HO/Giam sat CC/Chi nhanh)
    SUBSIDIARY, // ZTC_DTKT2 - AuditObjectSubsidiary (Cong ty con)
    PROCESS,    // ZTC_DTKT4 - AuditObjectProcess (Quy trinh)
    PROJECT     // ZTC_DTKT3 - AuditObjectProject (Du an/DVTN) - mac dinh cho category chua khai bao
}
