package com.govia.audit.khkt.khnsnam.dto;

import com.govia.audit.khkt.khnsnam.entity.AuditKhnsRoleInTeam;
import com.govia.identity.entity.EmployeeAuditorClassification;

import java.time.LocalDate;
import java.util.List;

/** 1 dong man hinh ZTC_KHNS_NAM = 1 nhan vien trong nam duoc chon (tu dong liet ke TOAN BO nhan
 * vien, khong can them/xoa dong - xem AuditKhnsNamService). Cac truong employeeCode..businessSegmentCode
 * doc song tu Employee; auditObjectCodes la NSD tu chon (K); auditObjectNames/objectBusinessSegmentCodes
 * tinh dong theo K, tra cuu tu TH2 cua nam nay. year/positions/monthAuditObjectNames phuc vu man hinh KHNS_NAM theo dac ta:
 * positionDetails = chuc vu (AuditKhnsPosition) + nghiep vu cua can bo o KHNS_PB, moi don vi khac nhau 1 phan tu (de hien y nguyen
 * cot "Chuc vu" cua man hinh do), monthAuditObjectNames = ten doi tuong KT
 * cua tung thang (12 phan tu, null neu thang khong di kiem toan). */
public record AuditKhnsNamRowResponse(
        String employeeId,
        String employeeCode,
        String employeeName,
        String positionName,
        String departmentCode,
        EmployeeAuditorClassification auditorClassification,
        String businessSegmentCode,
        boolean teamLeadCapable,
        AuditKhnsRoleInTeam roleInTeam,
        String otherDuties,
        List<String> auditObjectCodes,
        List<String> auditObjectNames,
        List<String> objectBusinessSegmentCodes,
        String decisionNumber,
        LocalDate decisionDate,
        String expectedBatch,
        Integer totalTeamsCount,
        String note,
        String month1AuditObjectCode,
        String month2AuditObjectCode,
        String month3AuditObjectCode,
        String month4AuditObjectCode,
        String month5AuditObjectCode,
        String month6AuditObjectCode,
        String month7AuditObjectCode,
        String month8AuditObjectCode,
        String month9AuditObjectCode,
        String month10AuditObjectCode,
        String month11AuditObjectCode,
        String month12AuditObjectCode,
        Integer year,
        List<PositionDetail> positionDetails,
        List<String> monthAuditObjectNames
) {
    /** Chuc vu + nghiep vu cua can bo tai 1 don vi o KHNS_PB - cung noi dung cot "Chuc vu" cua man hinh do. */
    public record PositionDetail(List<String> positions, List<String> segmentNames) {
    }
}
