package com.govia.audit.khkt.khnsnam.dto;

import com.govia.audit.khkt.khnsnam.entity.AuditKhnsRoleInTeam;

import java.util.List;

/** 1 dong man hinh KHNS_PB "Phan bo can bo cho chi nhanh theo thang" (sheet ZTC_KHNS_PB) = 1 can bo duoc
 * phan bo vao 1 don vi (doi tuong kiem toan). auditObjectName/businessSegmentCodes/creditScale/
 * fundingScale lay tu "Khai bao so thang kiem toan trong nam" (KHKT_THANG - doi tuong da phe duyet o
 * TH2); months = cac thang can bo do di kiem toan don vi nay (tu KHNS_NAM). segmentNames = ten cac
 * nghiep vu cua don vi ma can bo dam nhan duoc (giao cua nghiep vu don vi va kha nang dam nhan) - hien
 * kem cot "Chuc vu" de biet thanh vien lam nghiep vu nao. */
public record AuditKhnsPbRowResponse(
        String employeeId,
        String auditObjectCode,
        String auditObjectName,
        List<String> businessSegmentCodes,
        Integer creditScale,
        Integer fundingScale,
        String employeeCode,
        String employeeName,
        String username,
        AuditKhnsRoleInTeam roleInTeam,
        List<String> segmentNames,
        List<Integer> months
) {
}
