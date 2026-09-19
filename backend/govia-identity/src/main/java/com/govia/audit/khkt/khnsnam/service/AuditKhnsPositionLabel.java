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
}
