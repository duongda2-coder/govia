import { useEffect, useState } from "react";
import { Tag } from "antd";
import type { TFunction } from "i18next";
import dayjs from "dayjs";
import { getTdkpLookups, type TdkpDeadlineState, type TdkpLookups, type TdkpStatus, type TdkpTarget } from "../../../api/auditTdkp";

export const STATUS_VALUES: TdkpStatus[] = ["DONE", "IN_PROGRESS", "NOT_STARTED"];
export const TARGET_VALUES: TdkpTarget[] = ["HDTV", "TGD"];

const STATUS_COLORS: Record<TdkpStatus, string> = { DONE: "green", IN_PROGRESS: "blue", NOT_STARTED: "default" };

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

export const renderStatus = (t: TFunction) => (value: TdkpStatus | null) =>
  value ? <Tag color={STATUS_COLORS[value]}>{t(`auditTdkp.common.status.${value}`)}</Tag> : "-";

export const renderDeadlineState = (t: TFunction) => (value: TdkpDeadlineState | null) =>
  value ? <Tag color={value === "OVERDUE" ? "red" : "green"}>{t(`auditTdkp.common.deadline.${value}`)}</Tag> : "-";

export const renderDate = (value: string | null) => (value ? dayjs(value).format(DATE_DISPLAY) : "-");
export const renderText = (value: string | null | undefined) => value ?? "-";
export const toApiDate = (value: dayjs.Dayjs | null | undefined) => (value ? value.format(DATE_API) : null);
export const fromApiDate = (value: string | null | undefined) => (value ? dayjs(value) : undefined);

export const filterOption = (input: string, option?: { label?: unknown }) => String(option?.label ?? "").toLowerCase().includes(input.toLowerCase());
