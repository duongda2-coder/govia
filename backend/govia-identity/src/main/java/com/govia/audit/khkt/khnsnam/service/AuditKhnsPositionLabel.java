package com.govia.audit.khkt.khnsnam.service;

import com.govia.audit.khkt.khnsnam.entity.AuditKhnsPosition;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsRoleInTeam;

import java.util.List;
import java.util.stream.Collectors;

/** Chuoi hien thi cot "Chuc vu" cua KHNS_PB - giong y nguyen ham formatKhnsPositions o frontend (khnsPositionLabel.ts) va
 * nhan trong vi.json (auditKhnsPb.position.*, auditKhnsNam.role.*) de file Excel xuat ra khop voi man hinh. */
public final class AuditKhnsPositionLabel {

    private AuditKhnsPositionLabel() {
    }

    public static String position(AuditKhnsPosition position) {
        return switch (position) {
            case TEAM_LEAD -> "Trưởng đoàn";
            case QTDH_GROUP_LEAD -> "Trưởng nhóm QTĐH";
            case QTDH_MEMBER -> "Thành viên QTĐH";
            case TD_GROUP_LEAD -> "Trưởng nhóm Tín dụng";
            case TD_MEMBER -> "Thành viên Tín dụng";
            case NTD_GROUP_LEAD -> "Trưởng nhóm NTD";
            case NTD_MEMBER -> "Thành viên NTD";
        };
    }

    public static String role(AuditKhnsRoleInTeam role) {
        return switch (role) {
            case TEAM_LEAD -> "Trưởng đoàn";
            case GROUP_LEAD -> "Trưởng nhóm";
            case MEMBER -> "Thành viên";
            case SUPPORT -> "Cán bộ hỗ trợ";
        };
    }

    /** positions rong -> nhan cua chuc vu chung (roleInTeam) hoac null; chuc vu NTD ghi kem nghiep vu trong ngoac. */
    public static String format(List<String> positions, List<String> segmentNames, AuditKhnsRoleInTeam roleInTeam) {
        if (positions.isEmpty()) {
            return roleInTeam == null ? null : role(roleInTeam);
        }
        List<AuditKhnsPosition> parsed = positions.stream().map(AuditKhnsPosition::valueOf).toList();
        String labels = parsed.stream().map(AuditKhnsPositionLabel::position).collect(Collectors.joining(", "));
        boolean ntd = parsed.stream().anyMatch(AuditKhnsPosition::isNonCredit) && !segmentNames.isEmpty();
        return ntd ? labels + " (" + String.join(", ", segmentNames) + ")" : labels;
    }

    /** Vai tro cao nhat trong doan tu cac chuc vu chi tiet: Truong doan > Truong nhom (bat ky nhom nao) > Thanh vien; khong co chuc vu
     * chi tiet thi dung chuc vu chung. null neu khong xac dinh. Dung cho cot "Chuc vu" cua bao cao theo dot (ZTC_BC_DOT). */
    public static AuditKhnsRoleInTeam batchRoleKind(List<String> positions, AuditKhnsRoleInTeam roleInTeam) {
        if (positions.isEmpty()) {
            return roleInTeam;
        }
        List<AuditKhnsPosition> parsed = positions.stream().map(AuditKhnsPosition::valueOf).toList();
        if (parsed.contains(AuditKhnsPosition.TEAM_LEAD)) {
            return AuditKhnsRoleInTeam.TEAM_LEAD;
        }
        if (parsed.stream().anyMatch(p -> p.name().endsWith("_GROUP_LEAD"))) {
            return AuditKhnsRoleInTeam.GROUP_LEAD;
        }
        return AuditKhnsRoleInTeam.MEMBER;
    }

    /** Cot "Chuc vu" cua bao cao theo dot: Truong doan giu nguyen; Truong nhom / Thanh vien ghi kem linh vuc (QTĐH / TD / NTD) lay tu
     * "Linh vuc du kien duoc phan cong" cua can bo (xem {@link #group}), vd "Truong nhom NTD", "Thanh vien TD". */
    public static String batchRole(List<String> positions, AuditKhnsRoleInTeam roleInTeam, String group) {
        AuditKhnsRoleInTeam kind = batchRoleKind(positions, roleInTeam);
        if (kind == null) {
            return null;
        }
        boolean withGroup = kind == AuditKhnsRoleInTeam.GROUP_LEAD || kind == AuditKhnsRoleInTeam.MEMBER;
        return withGroup && group != null && !group.isBlank() ? role(kind) + " " + group : role(kind);
    }

    /** "Linh vuc duoc phan cong kiem toan" cua bao cao theo dot: nghiep vu du kien cua can bo CE -> QTĐH, LN -> TD, cac nghiep vu con
     * lai -> NTD; can bo chua co nghiep vu thi suy tu nhom chuc vu (QTĐH / Tin dung / NTD). */
    public static String group(String employeeSegmentCode, List<String> positions) {
        if (employeeSegmentCode != null && !employeeSegmentCode.isBlank()) {
            String code = employeeSegmentCode.trim();
            if (AuditKhnsPosition.QTDH_SEGMENT.equalsIgnoreCase(code)) {
                return "QTĐH";
            }
            return AuditKhnsPosition.CREDIT_SEGMENT.equalsIgnoreCase(code) ? "TD" : "NTD";
        }
        List<AuditKhnsPosition> parsed = positions.stream().map(AuditKhnsPosition::valueOf).toList();
        if (parsed.stream().anyMatch(p -> p == AuditKhnsPosition.QTDH_GROUP_LEAD || p == AuditKhnsPosition.QTDH_MEMBER)) {
            return "QTĐH";
        }
        if (parsed.stream().anyMatch(AuditKhnsPosition::isCredit)) {
            return "TD";
        }
        return parsed.stream().anyMatch(AuditKhnsPosition::isNonCredit) ? "NTD" : null;
    }
}
