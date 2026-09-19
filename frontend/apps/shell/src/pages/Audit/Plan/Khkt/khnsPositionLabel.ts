import type { TFunction } from "i18next";
import type { AuditKhnsRoleInTeam } from "../../../../api/auditKhnsNam";

/** Nội dung cột "Chức vụ" của cán bộ tại 1 đơn vị - dùng chung cho màn Phân bổ cán bộ cho chi nhánh theo tháng (KHNS_PB) và
 * "Chức vụ có thể đảm nhận trong đoàn" của màn Dự kiến nhân sự năm, đợt (KHNS_NAM) để 2 màn hiển thị y hệt nhau. */
export function formatKhnsPositions(
  t: TFunction,
  positions: string[],
  segmentNames: string[],
  roleInTeam: AuditKhnsRoleInTeam | null,
): string {
  if (positions.length === 0) return roleInTeam ? t(`auditKhnsNam.role.${roleInTeam}`) : "-";
  const labels = positions.map((p) => t(`auditKhnsPb.position.${p}`)).join(", ");
  // Chức vụ NTD ghi kèm các nghiệp vụ cụ thể cán bộ làm (tối đa 3)
  const ntd = positions.some((p) => p.startsWith("NTD_")) && segmentNames.length > 0;
  return ntd ? `${labels} (${segmentNames.join(", ")})` : labels;
}
