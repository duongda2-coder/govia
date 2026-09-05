import { useTranslation } from "react-i18next";
import { WorkManagementGridPage } from "./WorkManagementGridPage";

/** "Màn hình Quản lý công việc THKT" (sheet "Quản lý công việc" trong Tạo CKT (1).xlsx) - co them
 * nut "Báo cáo tiến độ" (Khối B) so voi man hinh CBKT. */
export function ThktWorkGridPage() {
  const { t } = useTranslation();
  return (
    <WorkManagementGridPage
      phase="THKT"
      tableId="audit.plan.execution.workManagement.thkt"
      title={t("auditWorkManagement.thktTitle")}
      showProgressReport
    />
  );
}
