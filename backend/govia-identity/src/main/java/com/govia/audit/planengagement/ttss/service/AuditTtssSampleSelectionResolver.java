package com.govia.audit.planengagement.ttss.service;

import com.govia.audit.cmntd1.entity.AuditCmNtd1;
import com.govia.audit.cmntd1.repository.AuditCmNtd1Repository;
import com.govia.audit.cmntd10.entity.AuditCmNtd10;
import com.govia.audit.cmntd10.repository.AuditCmNtd10Repository;
import com.govia.audit.cmntd11.entity.AuditCmNtd11;
import com.govia.audit.cmntd11.repository.AuditCmNtd11Repository;
import com.govia.audit.cmntd12.entity.AuditCmNtd12;
import com.govia.audit.cmntd12.repository.AuditCmNtd12Repository;
import com.govia.audit.cmntd2.entity.AuditCmNtd2;
import com.govia.audit.cmntd2.repository.AuditCmNtd2Repository;
import com.govia.audit.cmntd3.entity.AuditCmNtd3;
import com.govia.audit.cmntd3.repository.AuditCmNtd3Repository;
import com.govia.audit.cmntd4.entity.AuditCmNtd4;
import com.govia.audit.cmntd4.repository.AuditCmNtd4Repository;
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
 * file MAPPING.xlsx nguoi dung cung cap. CHI ap dung cho 7 nghiep vu da co man hinh nguon trong
 * GOVIA (GA, DP, CD, TF, AM, FA, LN) - MF (05D/BKS-KTNB) va DP-03G/BKS-KTNB chua co man hinh nen
 * khong co gi de doc; nghiep vu IT (CmNtd6 08B, CmNtd14 08C) cung khong dien vi 2 bang nguon do
 * khong co cot nao khop voi cac truong TTSS can dien.
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
    private final AuditCmNtd7Repository cmNtd7Repository;
    private final AuditCmNtd8Repository cmNtd8Repository;
    private final AuditCmNtd9Repository cmNtd9Repository;
    private final AuditCmNtd10Repository cmNtd10Repository;
    private final AuditCmNtd11Repository cmNtd11Repository;
    private final AuditCmNtd12Repository cmNtd12Repository;
    private final AuditCmTd1Repository cmTd1Repository;
    private final AuditCmTd2Repository cmTd2Repository;

    AuditTtssSampleSelectionResolver(AuditCmNtd1Repository cmNtd1Repository, AuditCmNtd2Repository cmNtd2Repository,
                                      AuditCmNtd3Repository cmNtd3Repository, AuditCmNtd4Repository cmNtd4Repository,
                                      AuditCmNtd7Repository cmNtd7Repository, AuditCmNtd8Repository cmNtd8Repository,
                                      AuditCmNtd9Repository cmNtd9Repository, AuditCmNtd10Repository cmNtd10Repository,
                                      AuditCmNtd11Repository cmNtd11Repository, AuditCmNtd12Repository cmNtd12Repository,
                                      AuditCmTd1Repository cmTd1Repository, AuditCmTd2Repository cmTd2Repository) {
        this.cmNtd1Repository = cmNtd1Repository;
        this.cmNtd2Repository = cmNtd2Repository;
        this.cmNtd3Repository = cmNtd3Repository;
        this.cmNtd4Repository = cmNtd4Repository;
        this.cmNtd7Repository = cmNtd7Repository;
        this.cmNtd8Repository = cmNtd8Repository;
        this.cmNtd9Repository = cmNtd9Repository;
        this.cmNtd10Repository = cmNtd10Repository;
        this.cmNtd11Repository = cmNtd11Repository;
        this.cmNtd12Repository = cmNtd12Repository;
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
            }
            case "CD" -> {
                cmNtd10Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromCd07b(r)));
                cmNtd12Repository.findByTenantIdAndEngagementIdOrderByCreatedAtAsc(tenantId, engagementId)
                        .forEach(r -> result.add(fromCd07f(r)));
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
            default -> {
                // MF (05D/BKS-KTNB), IT (08B/08C - khong co cot nao khop) va cac nghiep vu khac:
                // khong co man hinh nguon nao de doc, tra ve danh sach rong.
            }
        }
        return result;
    }

    private SampleFields fromGa04c(AuditCmNtd1 r) {
        return new SampleFields(bigDecimalRef(r.getEntryNumber()), dateRef(r.getTransactionDate()), r.getAccountNumber(), null,
                debitOrCredit(r.getDebitAmount(), r.getCreditAmount()), r.getPostingUser(), r.getContent(), r.getAssignedEmployeeId());
    }

    private SampleFields fromDp03e(AuditCmNtd2 r) {
        return new SampleFields(bigDecimalRef(r.getEntryNumber()), dateRef(r.getTransactionDate()), r.getAccountNumber(), null,
                r.getAmount(), r.getPostingUser(), null, r.getAssignedEmployeeId());
    }

    private SampleFields fromDp03c(AuditCmNtd3 r) {
        return new SampleFields(null, null, r.getCustomerCode(), r.getCustomerName(), r.getConvertedBalance(), null, null,
                r.getAssignedEmployeeId());
    }

    private SampleFields fromCd07b(AuditCmNtd10 r) {
        return new SampleFields(r.getAccountNumber(), dateRef(r.getIssueDate()), r.getCustomerCode(), r.getCustomerName(),
                r.getIssuanceFee(), r.getIssuingUser(), null, r.getAssignedEmployeeId());
    }

    private SampleFields fromCd07f(AuditCmNtd12 r) {
        return new SampleFields(bigDecimalRef(r.getEntryNumber()), dateRef(r.getTransactionDate()), r.getAccountNumber(), null,
                debitOrCredit(r.getDebitAmount(), r.getCreditAmount()), r.getPostingUser(), r.getContent(), r.getAssignedEmployeeId());
    }

    private SampleFields fromTf06b(AuditCmNtd4 r) {
        return new SampleFields(bigDecimalRef(r.getReferenceNumber()), dateRef(r.getOpenDate()), r.getCorebankCustomerCode(), null,
                r.getAmount(), null, null, r.getAssignedEmployeeId());
    }

    private SampleFields fromTf06c(AuditCmNtd11 r) {
        return new SampleFields(r.getReferenceNumber(), dateRef(r.getTransactionDate()), r.getCustomerCode(), r.getCustomerName(),
                r.getAmount(), r.getTransactionStaff(), null, r.getAssignedEmployeeId());
    }

    private SampleFields fromAm13b(AuditCmNtd8 r) {
        return new SampleFields(r.getReferenceNumber(), dateRef(r.getTransactionDate()), null, null,
                r.getAmount(), r.getPostingUser(), null, r.getAssignedEmployeeId());
    }

    private SampleFields fromAm13a(AuditCmNtd9 r) {
        return new SampleFields(null, null, r.getCustomerCode(), r.getCustomerName(), null, r.getPostingUser(),
                r.getTransactionContent(), r.getAssignedEmployeeId());
    }

    private SampleFields fromFa09b(AuditCmNtd7 r) {
        return new SampleFields(null, null, null, null, null, null, r.getContent(), r.getAssignedEmployeeId());
    }

    private SampleFields fromLn02l(AuditCmTd1 r) {
        return new SampleFields(null, null, r.getCustomerCode(), r.getCustomerName(), null, null, r.getLoanPurpose(),
                r.getAssignedEmployeeId());
    }

    private SampleFields fromLn02g(AuditCmTd2 r) {
        return new SampleFields(bigDecimalRef(r.getEntryNumber()), dateRef(r.getTransactionDate()), r.getCustomerCode(), r.getCustomerName(),
                debitOrCredit(r.getDebitAmount(), r.getCreditAmount()), r.getPostingUser(), null, r.getAssignedEmployeeId());
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
