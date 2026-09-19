package com.govia.audit.khkt.khnsnam.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Cac truong nhap tay o man hinh KHNS_NAM (phan bo thang/chuc vu lay tu KHNS_PB nen khong sua o day). So/ngay quyet dinh,
 * dot du kien va ghi chu khong hien thanh cot theo dac ta nhung "Chuyen thong tin KHTH" van can, nen giu o form Sua. */
public record AuditKhnsNamInfoRequest(
        @Size(max = 120) String otherDuties,
        @Size(max = 25) String decisionNumber,
        LocalDate decisionDate,
        @Size(max = 100) String expectedBatch,
        @Size(max = 120) String note
) {
}
