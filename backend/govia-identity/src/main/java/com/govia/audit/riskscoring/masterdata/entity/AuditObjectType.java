package com.govia.audit.riskscoring.masterdata.entity;

/**
 * Loai bang cua "Doi tuong kiem toan" ma Group1/CriteriaQualitative/CriteriaQuantitative tham chieu
 * toi (UUID cu the trong 1 trong 4 danh muc ZTC_DTKT1-4) - khong dung 1 FK DB duy nhat duoc vi moi
 * loai tro toi 1 bang khac nhau, nen luu discriminator nay + UUID rồi validate/resolve o tang service
 * (xem AuditObjectReferenceService).
 */
public enum AuditObjectType {
    UNIT,
    SUBSIDIARY,
    PROJECT,
    PROCESS
}
