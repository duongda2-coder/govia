package com.govia.audit.planengagement.ttss.service;

import com.govia.audit.cmntd1.entity.AuditCmNtd1;
import com.govia.audit.cmntd1.repository.AuditCmNtd1Repository;
import com.govia.audit.cmntd10.entity.AuditCmNtd10;
import com.govia.audit.cmntd10.repository.AuditCmNtd10Repository;
import com.govia.audit.cmntd11.entity.AuditCmNtd11;
import com.govia.audit.cmntd11.repository.AuditCmNtd11Repository;
import com.govia.audit.cmntd12.entity.AuditCmNtd12;
import com.govia.audit.cmntd12.repository.AuditCmNtd12Repository;
import com.govia.audit.cmntd13.entity.AuditCmNtd13;
import com.govia.audit.cmntd13.repository.AuditCmNtd13Repository;
import com.govia.audit.cmntd14.entity.AuditCmNtd14;
import com.govia.audit.cmntd14.repository.AuditCmNtd14Repository;
import com.govia.audit.cmntd15.entity.AuditCmNtd15;
import com.govia.audit.cmntd15.repository.AuditCmNtd15Repository;
import com.govia.audit.cmntd16.entity.AuditCmNtd16;
import com.govia.audit.cmntd16.repository.AuditCmNtd16Repository;
import com.govia.audit.cmntd2.entity.AuditCmNtd2;
import com.govia.audit.cmntd2.repository.AuditCmNtd2Repository;
import com.govia.audit.cmntd3.entity.AuditCmNtd3;
import com.govia.audit.cmntd3.repository.AuditCmNtd3Repository;
import com.govia.audit.cmntd4.entity.AuditCmNtd4;
import com.govia.audit.cmntd4.repository.AuditCmNtd4Repository;
import com.govia.audit.cmntd6.entity.AuditCmNtd6;
import com.govia.audit.cmntd6.repository.AuditCmNtd6Repository;
import com.govia.audit.cmntd7.entity.AuditCmNtd7;
import com.govia.audit.cmntd7.repository.AuditCmNtd7Repository;
import com.govia.audit.cmntd8.entity.AuditCmNtd8;
import com.govia.audit.cmntd8.repository.AuditCmNtd8Repository;
import com.govia.audit.cmntd9.entity.AuditCmNtd9;
import com.govia.audit.cmntd9.repository.AuditCmNtd9Repository;
import com.govia.audit.cmtd1.entity.AuditCmTd1;
import com.govia.audit.cmtd1.repository.AuditCmTd1Repository;
import com.govia.audit.cmtd2.entity.AuditCmTd2;
import com.govia.audit.cmtd2.repository.AuditCmTd2Repository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tu dong dien 1 vai cot TTSS con trong ("Số tham chiếu", "Số tham chiếu 2", "Mã khách hàng",
 * "Tên KH", "Số tiền", "User thực hiện", "Nội dung giao dịch") tu du lieu da co san o man hinh
 * "chon mau" rieng cua tung nghiep vu (CmTd1/2, CmNtd1..14), theo dung mapping nghiep vu trong
 * file MAPPING.xlsx nguoi dung cung cap (STT 1-9 trong file, moi STT ung voi 1 nghiep vu, co the
 * gom nhieu ma man hinh nguon). Ap dung cho CA 9 nghiep vu (GA, DP, CD, IT, TF, AM, FA, LN, MF) -
 * ke ca MF (05D/BKS-KTNB, AuditCmNtd15) va DP-03G/BKS-KTNB (AuditCmNtd16), 2 man hinh duoc bo sung
 * sau (xem ZTC_CM_NTD15,16.xlsx nguoi dung cung cap) theo dung cau truc AuditCmNtd1 (04C/GA).
 *
 * <p>1 vai o trong MAPPING.xlsx tro toi cot khong ton tai tren bang nguon tuong ung trong GOVIA
 * (vd DP-03E/03C thieu cot "ten nguoi/don vi thu huong" va "noi dung giao dich"; AM-13A thieu cot
 * "So CMND"; LN-02G (AuditCmTd2) khong co cot "muc dich vay von" - chi AuditCmTd1/02L co) - CHI
 * NHUNG o do moi CO CHU DINH de trong, khac voi loi thieu-wiring da sua (vd DP-03C tung doc nham
 * convertedBalance thay vi originalCurrencyBalance cho cot "số dư nguyên tệ").
 *
 * <p>KHONG co khoa lien ket dang tin cay giua 1 cong viec TTSS va DUNG dong chon mau tuong ung
 * (cac bang chon mau chi co engagementId/assignedEmployeeId/processStepSummaryId/branchCode,
 * khong co processStepDetailId nhu khoa tu nhien cua TTSS; AuditWorkItem.detailCode - truong co
 * ve danh cho viec nay - la o nhap tu do, chua tung duoc gan gia tri khop ten module). Vi vay CHI
 * tu dong dien khi so dong "ung vien" (tat ca dong thuoc nghiep vu do trong CKT nay, uu tien loc
 * hep hon theo assignedEmployeeId neu co) thu duoc DUNG 1 dong - con lai (0 hoac nhieu hon 1) thi
 * de trong y nguyen nhu truoc, tranh dien nham.
 */
@Component
class AuditTtssSampleSelectionResolver {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    record SampleFields(String referenceNumber, String referenceNumber2, String customerCode, String customerName,
                         BigDecimal amount, String performingUser, String transactionContent, UUID assignedEmployeeId) {
    }

    private final AuditCmNtd1Repository cmNtd1Repository;
    private final AuditCmNtd2Repository cmNtd2Repository;
    private final AuditCmNtd3Repository cmNtd3Repository;
    private final AuditCmNtd4Repository cmNtd4Repository;
    private final AuditCmNtd6Repository cmNtd6Repository;
    private final AuditCmNtd7Repository cmNtd7Repository;
    private final AuditCmNtd8Repository cmNtd8Repository;
    private final AuditCmNtd9Repository cmNtd9Repository;
    private final AuditCmNtd10Repository cmNtd10Repository;
    private final AuditCmNtd11Repository cmNtd11Repository;
    private final AuditCmNtd12Repository cmNtd12Repository;
    private final AuditCmNtd13Repository cmNtd13Repository;
    private final AuditCmNtd14Repository cmNtd14Repository;
    private final AuditCmNtd15Repository cmNtd15Repository;
    private final AuditCmNtd16Repository cmNtd16Repository;
    private final AuditCmTd1Repository cmTd1Repository;
    private final AuditCmTd2Repository cmTd2Repository;

    AuditTtssSampleSelectionResolver(AuditCmNtd1Repository cmNtd1Repository, AuditCmNtd2Repository cmNtd2Repository,
                                      AuditCmNtd3Repository cmNtd3Repository, AuditCmNtd4Repository cmNtd4Repository,
                                      AuditCmNtd6Repository cmNtd6Repository, AuditCmNtd7Repository cmNtd7Repository,
                                      AuditCmNtd8Repository cmNtd8Repository, AuditCmNtd9Repository cmNtd9Repository,
                                      AuditCmNtd10Repository cmNtd10Repository, AuditCmNtd11Repository cmNtd11Repository,
                                      AuditCmNtd12Repository cmNtd12Repository, AuditCmNtd13Repository cmNtd13Repository,
                                      AuditCmNtd14Repository cmNtd14Repository, AuditCmNtd15Repository cmNtd15Repository,
                                      AuditCmNtd16Repository cmNtd16Repository, AuditCmTd1Repository cmTd1Repository,
                                      AuditCmTd2Repository cmTd2Repository) {
        this.cmNtd1Repository = cmNtd1Repository;
        this.cmNtd2Repository = cmNtd2Repository;
        this.cmNtd3Repository = cmNtd3Repository;
        this.cmNtd4Repository = cmNtd4Repository;
        this.cmNtd6Repository = cmNtd6Repository;
        this.cmNtd7Repository = cmNtd7Repository;
        this.cmNtd8Repository = cmNtd8Repository;
        this.cmNtd9Repository = cmNtd9Repository;
        this.cmNtd10Repository = cmNtd10Repository;
        this.cmNtd11Repository = cmNtd11Repository;
        this.cmNtd12Repository = cmNtd12Repository;
        this.cmNtd13Repository = cmNtd13Repository;
        this.cmNtd14Repository = cmNtd14Repository;
        this.cmNtd15Repository = cmNtd15Repository;
        this.cmNtd16Repository = cmNtd16Repository;
        this.cmTd1Repository = cmTd1Repository;
        this.cmTd2Repository = cmTd2Repository;
    }

    /** Tra ve dong chon mau DUY NHAT khop voi (tenantId, engagementId, segmentCode), uu tien loc
     * theo assignmentEmployeeId neu viec loc do van con it nhat 1 ung vien - Optional.empty() neu
     * khong co hoac co nhieu hon 1 ung vien (mo ho, khong tu dien). */
    Optional<SampleFields> resolveUnique(UUID tenantId, UUID engagementId, String segmentCode, UUID assignmentEmployeeId) {
        List<SampleFields> candidates = candidatesFor(tenantId, engagementId, segmentCode);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        if (assignmentEmployeeId != null) {
            List<SampleFields> narrowed = candidates.stream().filter(c -> assignmentEmployeeId.equals(c.assignedEmployeeId())).toList();
            if (!narrowed.isEmpty()) {
                candidates = narrowed;
            }
        }
        return candidates.size() == 1 ? Optional.of(candidates.get(0)) : Optional.empty();
    }

    private List<SampleFields> candidatesFor(UUID tenantId, UUID engagementId, String segmentCode) {
        List<SampleFields> result = new ArrayList<>();
        if (segmentCode == null) {
            return result;
        }
        switch (segmentCode) {
            case "GA" -> cmNtd1Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                    .forEach(r -> result.add(fromGa04c(r)));
            case "DP" -> {
                cmNtd2Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromDp03e(r)));
                cmNtd3Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromDp03c(r)));
                cmNtd16Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromDp03g(r)));
            }
            case "CD" -> {
                cmNtd10Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromCd07b(r)));
                cmNtd12Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromCd07f(r)));
                cmNtd13Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromCd07d(r)));
            }
            case "IT" -> {
                cmNtd6Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromIt08b(r)));
                cmNtd14Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromIt08c(r)));
            }
            case "TF" -> {
                cmNtd4Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromTf06b(r)));
                cmNtd11Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromTf06c(r)));
            }
            case "AM" -> {
                cmNtd8Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromAm13b(r)));
                cmNtd9Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromAm13a(r)));
            }
            case "FA" -> cmNtd7Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                    .forEach(r -> result.add(fromFa09b(r)));
            case "LN" -> {
                cmTd1Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromLn02l(r)));
                cmTd2Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromLn02g(r)));
            }
            case "MF" -> cmNtd15Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                    .forEach(r -> result.add(fromMf05d(r)));
            default -> {
                // Nghiep vu khac chua co man hinh nguon nao trong GOVIA: tra ve danh sach rong.
            }
        }
        return result;
    }

    private SampleFields fromGa04c(AuditCmNtd1 r) {
        return new SampleFields(bigDecimalRef(r.getEntryNumber()), dateRef(r.getTransactionDate()), r.getAccountNumber(), null,
                debitOrCredit(r.getDebitAmount(), r.getCreditAmount()), r.getPostingUser(), r.getContent(), r.getAssignedEmployeeId());
    }

    /** MAPPING.xlsx (03E/BKS-KTNB) cung yeu cau "Tên KH" <- "Tên TH ( nếu có )" va "Nội dung giao
     * dịch" <- "Nội dung giao dịch trong mẫu", nhung AuditCmNtd2 khong co cot ten nguoi/don vi thu
     * huong hay noi dung giao dich (man hinh nay chi ghi nhan but toan: so/ngay/user/so tien/tai
     * khoan) - 2 truong do CO CHU DINH de trong, khac voi cac o mapping khac tren cung dong da doc
     * duoc du lieu that (entryNumber/transactionDate/accountNumber/amount/postingUser). */
    private SampleFields fromDp03e(AuditCmNtd2 r) {
        return new SampleFields(bigDecimalRef(r.getEntryNumber()), dateRef(r.getTransactionDate()), r.getAccountNumber(), null,
                r.getAmount(), r.getPostingUser(), null, r.getAssignedEmployeeId());
    }

    /** MAPPING.xlsx (03G/BKS-KTNB, NTD16) - cung 6 cot mapping voi 04C/GA va 05D/MF (Số tham chiếu
     * <- Số bút toán, Số tham chiếu 2 <- Ngày giao dịch, Số tiền <- phát sinh nợ/có, User thực hiện
     * <- User hạch toán, Mã khách hàng <- Tài khoản hạch toán, Nội dung giao dịch <- Nội dung). */
    private SampleFields fromDp03g(AuditCmNtd16 r) {
        return new SampleFields(bigDecimalRef(r.getEntryNumber()), dateRef(r.getTransactionDate()), r.getAccountNumber(), null,
                debitOrCredit(r.getDebitAmount(), r.getCreditAmount()), r.getPostingUser(), r.getContent(), r.getAssignedEmployeeId());
    }

    /** MAPPING.xlsx (03C/BKS-KTNB) khong co cot nao khop "Số tham chiếu"/"Số tham chiếu 2"/"User thực
     * hiện"/"Nội dung giao dịch" tren AuditCmNtd3 (man hinh nay khong co truong ngay/tai khoan/user/
     * noi dung) nen 4 truong do CO CHU DINH de trong. "Số tiền" doc dung "số dư nguyên tệ" =
     * originalCurrencyBalance (KHONG PHAI convertedBalance - 2 cot khac nhau tren cung 1 bang). */
    private SampleFields fromDp03c(AuditCmNtd3 r) {
        return new SampleFields(null, null, r.getCustomerCode(), r.getCustomerName(), r.getOriginalCurrencyBalance(), null, null,
                r.getAssignedEmployeeId());
    }

    /** MAPPING.xlsx: "Nội dung giao dịch" <- "Hạn thẻ" - AuditCmNtd10 khong co cot han/ngay het han
     * the, chi co cardTier ("Hạng thẻ") gan nghia nhat (lech 1 dau so voi ban mapping) nen dung tam. */
    private SampleFields fromCd07b(AuditCmNtd10 r) {
        return new SampleFields(r.getAccountNumber(), dateRef(r.getIssueDate()), r.getCustomerCode(), r.getCustomerName(),
                r.getIssuanceFee(), r.getIssuingUser(), r.getCardTier(), r.getAssignedEmployeeId());
    }

    private SampleFields fromCd07f(AuditCmNtd12 r) {
        return new SampleFields(bigDecimalRef(r.getEntryNumber()), dateRef(r.getTransactionDate()), r.getAccountNumber(), null,
                debitOrCredit(r.getDebitAmount(), r.getCreditAmount()), r.getPostingUser(), r.getContent(), r.getAssignedEmployeeId());
    }

    /** MAPPING.xlsx (07D/BKS-KTNB, NTD13) - "Số tiền" khong co dong mapping (de trong). */
    private SampleFields fromCd07d(AuditCmNtd13 r) {
        return new SampleFields(r.getMerchantAccountNumber(), dateRef(r.getOccurrenceDate()), r.getMerchantId(),
                r.getBusinessRegistrationName(), null, null, r.getSampleReason(), r.getAssignedEmployeeId());
    }

    /** MAPPING.xlsx (08B/BKS-KTNB, NTD6) - "Số tham chiếu 2" khong co dong mapping (de trong). */
    private SampleFields fromIt08b(AuditCmNtd6 r) {
        return new SampleFields(r.getSampleCode(), null, r.getStaffCode(), r.getStaffName(),
                null, r.getIpcasUser(), r.getSecurityDevice(), r.getAssignedEmployeeId());
    }

    /** MAPPING.xlsx (08C/BKS-KTNB, NTD14) - "Số tham chiếu" ("mã mẫu chọn") khong co cot tuong ung
     * tren AuditCmNtd14 (man hinh nay khong co ma mau chon rieng) nen CO CHU DINH de trong. */
    private SampleFields fromIt08c(AuditCmNtd14 r) {
        return new SampleFields(null, dateRef(r.getAttendanceDate()), r.getStaffCode(), r.getStaffName(),
                null, r.getUserCode(), r.getSampleReason(), r.getAssignedEmployeeId());
    }

    /** MAPPING.xlsx: "Tên KH" <- "Người thụ hưởng" (beneficiary), "Nội dung giao dịch" <- "Lý do
     * chọn mẫu" (sampleReason) - ca 2 deu co tren AuditCmNtd4. */
    private SampleFields fromTf06b(AuditCmNtd4 r) {
        return new SampleFields(bigDecimalRef(r.getReferenceNumber()), dateRef(r.getOpenDate()), r.getCorebankCustomerCode(),
                r.getBeneficiary(), r.getAmount(), null, r.getSampleReason(), r.getAssignedEmployeeId());
    }

    /** MAPPING.xlsx: "Nội dung giao dịch" <- "Lý do chọn mẫu" (sampleReason). */
    private SampleFields fromTf06c(AuditCmNtd11 r) {
        return new SampleFields(r.getReferenceNumber(), dateRef(r.getTransactionDate()), r.getCustomerCode(), r.getCustomerName(),
                r.getAmount(), r.getTransactionStaff(), r.getSampleReason(), r.getAssignedEmployeeId());
    }

    /** MAPPING.xlsx (13B/BKS-KTNB): "Mã khách hàng" <- "Số bút toán" (entryNumber), "Tên KH" <- "Đơn
     * vị phát lệnh" (orderingParty), "Nội dung giao dịch" <- "Lý do chọn mẫu" (sampleReason). */
    private SampleFields fromAm13b(AuditCmNtd8 r) {
        return new SampleFields(r.getReferenceNumber(), dateRef(r.getTransactionDate()), bigDecimalRef(r.getEntryNumber()),
                r.getOrderingParty(), r.getAmount(), r.getPostingUser(), r.getSampleReason(), r.getAssignedEmployeeId());
    }

    /** MAPPING.xlsx (13A/BKS-KTNB): "Số tham chiếu" <- "Số CMND" - AuditCmNtd9 khong co cot so CMND
     * nen CO CHU DINH de trong; "Số tham chiếu 2" <- "Ngày giao dịch thực tế" = transactionDate. */
    private SampleFields fromAm13a(AuditCmNtd9 r) {
        return new SampleFields(null, dateRef(r.getTransactionDate()), r.getCustomerCode(), r.getCustomerName(), null,
                r.getPostingUser(), r.getTransactionContent(), r.getAssignedEmployeeId());
    }

    /** MAPPING.xlsx (09B/BKS-KTNB): "Mã khách hàng" <- "Số tham chiếu (mã CT)" (constructionCode),
     * "Tên KH" <- "Tên Công trình" (constructionName). */
    private SampleFields fromFa09b(AuditCmNtd7 r) {
        return new SampleFields(null, null, r.getConstructionCode(), r.getConstructionName(), null, null, r.getContent(),
                r.getAssignedEmployeeId());
    }

    private SampleFields fromLn02l(AuditCmTd1 r) {
        return new SampleFields(null, null, r.getCustomerCode(), r.getCustomerName(), null, null, r.getLoanPurpose(),
                r.getAssignedEmployeeId());
    }

    /** MAPPING.xlsx gop chung 1 nhom mapping cho ca 02L va 02G, CHI chi dinh 3 cot ("Mã khách hàng",
     * "Tên KH", "Nội dung giao dịch") - "Số tham chiếu"/"Số tham chiếu 2"/"Số tiền"/"User thực hiện"
     * KHONG co dong mapping nen CO CHU DINH de trong cho ca 2 form (khac voi cac nghiep vu khac,
     * "Nội dung giao dịch" <- "Mục đích vay vốn" nhung AuditCmTd2 khong co cot loan purpose - chi
     * AuditCmTd1/02L moi co - nen de trong tren 02G). */
    private SampleFields fromLn02g(AuditCmTd2 r) {
        return new SampleFields(null, null, r.getCustomerCode(), r.getCustomerName(), null, null, null, r.getAssignedEmployeeId());
    }

    /** MAPPING.xlsx (05D/BKS-KTNB, NTD15) - cung 6 cot mapping voi 04C/GA va 03G/DP (xem fromDp03g). */
    private SampleFields fromMf05d(AuditCmNtd15 r) {
        return new SampleFields(bigDecimalRef(r.getEntryNumber()), dateRef(r.getTransactionDate()), r.getAccountNumber(), null,
                debitOrCredit(r.getDebitAmount(), r.getCreditAmount()), r.getPostingUser(), r.getContent(), r.getAssignedEmployeeId());
    }

    private static BigDecimal debitOrCredit(BigDecimal debit, BigDecimal credit) {
        return debit != null && debit.compareTo(BigDecimal.ZERO) != 0 ? debit : credit;
    }

    private static String bigDecimalRef(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private static String dateRef(LocalDate value) {
        return value == null ? null : value.format(DATE_FORMAT);
    }
}
