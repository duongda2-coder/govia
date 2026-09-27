import { useEffect, useState } from "react";
import { Select, Space, Tag, Typography } from "antd";
import type { TFunction } from "i18next";
import dayjs from "dayjs";
import { getTdkpLookups, type TdkpDeadlineState, type TdkpLookups, type TdkpStatus, type TdkpTarget } from "../../../api/auditTdkp";
import type { AssignmentApprovalStatus } from "../../../api/auditWorkManagement";

export const STATUS_VALUES: TdkpStatus[] = ["DONE", "IN_PROGRESS", "NOT_STARTED"];
export const TARGET_VALUES: TdkpTarget[] = ["HDTV", "TGD"];
export const APPROVAL_STATUS_VALUES: AssignmentApprovalStatus[] = ["PENDING", "APPROVED", "REJECTED"];

const STATUS_COLORS: Record<TdkpStatus, string> = { DONE: "green", IN_PROGRESS: "blue", NOT_STARTED: "default" };
const APPROVAL_STATUS_COLORS: Record<AssignmentApprovalStatus, string> = { PENDING: "processing", APPROVED: "success", REJECTED: "error" };

export const DATE_DISPLAY = "DD.MM.YYYY";
export const DATE_API = "YYYY-MM-DD";

/** Danh sách chọn dùng chung (Phân loại KN, Mảng NV, Khu vực địa lý, Đối tượng kiểm toán, Cán bộ KTNB) - tải 1 lần khi mở màn hình. */
export function useTdkpLookups(onError: () => void) {
  const [lookups, setLookups] = useState<TdkpLookups>({ recommendationTypes: [], businessSegments: [], geographicAreas: [], units: [], employees: [] });
  useEffect(() => {
    getTdkpLookups().then(setLookups).catch(onError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return lookups;
}

export const statusOptions = (t: TFunction) => STATUS_VALUES.map((value) => ({ value, label: t(`auditTdkp.common.status.${value}`) }));
export const targetOptions = (t: TFunction) => TARGET_VALUES.map((value) => ({ value, label: t(`auditTdkp.common.target.${value}`) }));
export const approvalStatusOptions = (t: TFunction) => APPROVAL_STATUS_VALUES.map((value) => ({ value, label: t(`auditWorkManagement.approvalStatus.${value}`) }));

export const renderStatus = (t: TFunction) => (value: TdkpStatus | null) =>
  value ? <Tag color={STATUS_COLORS[value]}>{t(`auditTdkp.common.status.${value}`)}</Tag> : "-";

export const renderApprovalStatus = (t: TFunction) => (value: AssignmentApprovalStatus | null) =>
  value ? <Tag color={APPROVAL_STATUS_COLORS[value]}>{t(`auditWorkManagement.approvalStatus.${value}`)}</Tag> : "-";

export const renderDeadlineState = (t: TFunction) => (value: TdkpDeadlineState | null) =>
  value ? <Tag color={value === "OVERDUE" ? "red" : "green"}>{t(`auditTdkp.common.deadline.${value}`)}</Tag> : "-";

export const renderDate = (value: string | null) => (value ? dayjs(value).format(DATE_DISPLAY) : "-");
export const renderText = (value: string | null | undefined) => value ?? "-";
export const toApiDate = (value: dayjs.Dayjs | null | undefined) => (value ? value.format(DATE_API) : null);
export const fromApiDate = (value: string | null | undefined) => (value ? dayjs(value) : undefined);

export const filterOption = (input: string, option?: { label?: unknown }) => String(option?.label ?? "").toLowerCase().includes(input.toLowerCase());

export type CompletionClosedFilter = "CLOSED" | "NOT_CLOSED";

/** test25.9: "Đóng thời hạn hoàn thành" khi ngày < ngày hiện tại, còn lại (kể cả chưa nhập ngày) là "Chưa đóng". */
export const isCompletionClosed = (value: string | null | undefined) => !!value && dayjs(value).isBefore(dayjs(), "day");

export function filterByCompletionClosed<T>(items: T[], filter: CompletionClosedFilter | undefined, dateOf: (item: T) => string | null | undefined): T[] {
  if (!filter) return items;
  return items.filter((item) => isCompletionClosed(dateOf(item)) === (filter === "CLOSED"));
}

/** Bộ lọc "Đóng / Chưa đóng thời hạn hoàn thành" đặt trên bảng. */
export function CompletionClosedFilterSelect({ t, value, onChange }: { t: TFunction; value: CompletionClosedFilter | undefined; onChange: (value: CompletionClosedFilter | undefined) => void }) {
  return (
    <Space style={{ marginBottom: 16 }} wrap>
      <Typography.Text>{t("auditTdkp.common.completionClosed.label")}</Typography.Text>
      <Select<CompletionClosedFilter>
        style={{ width: 260 }}
        allowClear
        placeholder={t("auditTdkp.common.completionClosed.all")}
        options={(["NOT_CLOSED", "CLOSED"] as const).map((v) => ({ value: v, label: t(`auditTdkp.common.completionClosed.${v}`) }))}
        value={value}
        onChange={onChange}
      />
    </Space>
  );
}
