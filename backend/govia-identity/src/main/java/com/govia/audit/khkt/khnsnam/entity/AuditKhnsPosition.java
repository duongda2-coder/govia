package com.govia.audit.khkt.khnsnam.entity;

import com.govia.core.web.BusinessException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** "Chuc vu" cua can bo tai 1 don vi o man hinh KHNS_PB - 1 can bo co the giu nhieu chuc vu cung luc (tich chon).
 * Doan kiem toan chia 3 nhom: QTDH (nghiep vu CE), Tin dung (LN) va NTD = ngoai tin dung (cac nghiep vu con lai). */
public enum AuditKhnsPosition {
    TEAM_LEAD,
    QTDH_GROUP_LEAD,
    QTDH_MEMBER,
    TD_GROUP_LEAD,
    TD_MEMBER,
    NTD_GROUP_LEAD,
    NTD_MEMBER;

    public static final String QTDH_SEGMENT = "CE";
    public static final String CREDIT_SEGMENT = "LN";
    /** 1 can bo chi lam toi da 3 nghiep vu tai 1 don vi. */
    public static final int MAX_SEGMENTS = 3;

    public boolean isCredit() {
        return this == TD_GROUP_LEAD || this == TD_MEMBER;
    }

    public boolean isNonCredit() {
        return this == NTD_GROUP_LEAD || this == NTD_MEMBER;
    }

    public static String join(Collection<AuditKhnsPosition> positions) {
        return positions.stream().sorted().map(Enum::name).collect(Collectors.joining(","));
    }

    /** Bo qua gia tri la (du lieu cu/sai) thay vi bao loi khi doc. */
    public static Set<AuditKhnsPosition> parse(String csv) {
        Set<AuditKhnsPosition> result = EnumSet.noneOf(AuditKhnsPosition.class);
        if (csv != null) {
            for (String part : csv.split(",")) {
                try {
                    result.add(valueOf(part.trim()));
                } catch (IllegalArgumentException ignored) {
                    // gia tri khong hop le -> bo qua
                }
            }
        }
        return result;
    }

    public static List<String> parseSegments(String csv) {
        List<String> result = new ArrayList<>();
        if (csv != null) {
            for (String part : csv.split(",")) {
                if (!part.isBlank()) {
                    result.add(part.trim());
                }
            }
        }
        return result;
    }

    /** Quy tac theo yeu cau: 1 nguoi lam toi da 3 nghiep vu, va chi lam Tin dung HOAC NTD (khong lam ca 2). */
    public static void validate(Collection<AuditKhnsPosition> positions, Collection<String> segmentCodes) {
        boolean credit = positions.stream().anyMatch(AuditKhnsPosition::isCredit);
        boolean nonCredit = positions.stream().anyMatch(AuditKhnsPosition::isNonCredit);
        if (credit && nonCredit) {
            throw new BusinessException("AUDIT_KHNS_PB_CREDIT_AND_NTD", "Mot can bo chi lam Tin dung hoac NTD, khong lam ca hai");
        }
        if (segmentCodes.size() > MAX_SEGMENTS) {
            throw new BusinessException("AUDIT_KHNS_PB_TOO_MANY_SEGMENTS", "Mot can bo chi lam toi da " + MAX_SEGMENTS + " nghiep vu");
        }
        boolean creditSegment = segmentCodes.contains(CREDIT_SEGMENT);
        boolean nonCreditSegment = segmentCodes.stream().anyMatch(c -> !CREDIT_SEGMENT.equals(c) && !QTDH_SEGMENT.equals(c));
        if (creditSegment && nonCreditSegment || credit && nonCreditSegment || nonCredit && creditSegment) {
            throw new BusinessException("AUDIT_KHNS_PB_CREDIT_AND_NTD", "Mot can bo chi lam Tin dung hoac NTD, khong lam ca hai");
        }
    }
}
