import { useEffect, useState } from "react";
import type { TFunction } from "i18next";
import dayjs from "dayjs";
import {
  getPhbcLookups,
  type PhbcLookups,
  type PhbcRecommendationCode,
  type PhbcRecommendationTarget,
} from "../../../api/auditPhbc";

export const RECOMMENDATION_CODE_VALUES: PhbcRecommendationCode[] = ["M01", "M02", "M03", "M04"];
export const TARGET_VALUES: PhbcRecommendationTarget[] = ["HDTV", "TGD", "ALL"];

export const DATE_DISPLAY = "DD.MM.YYYY";
export const DATE_API = "YYYY-MM-DD";

/** Danh sách chọn dùng chung (Mảng nghiệp vụ, Đơn vị thực hiện) - tải 1 lần khi mở màn hình. */
export function usePhbcLookups(onError: () => void) {
  const [lookups, setLookups] = useState<PhbcLookups>({ businessSegments: [], executingUnits: [] });
  useEffect(() => {
    getPhbcLookups().then(setLookups).catch(onError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return lookups;
}

export const recommendationCodeOptions = (t: TFunction) =>
  RECOMMENDATION_CODE_VALUES.map((value) => ({ value, label: `${value} - ${t(`auditPhbc.common.recommendationCode.${value}`)}` }));

export const targetOptions = (t: TFunction) => TARGET_VALUES.map((value) => ({ value, label: t(`auditPhbc.common.target.${value}`) }));

export const renderDate = (value: string | null) => (value ? dayjs(value).format(DATE_DISPLAY) : "-");
export const renderText = (value: string | null | undefined) => value ?? "-";
export const toApiDate = (value: dayjs.Dayjs | null | undefined) => (value ? value.format(DATE_API) : null);
export const fromApiDate = (value: string | null | undefined) => (value ? dayjs(value) : undefined);

export const filterOption = (input: string, option?: { label?: unknown }) => String(option?.label ?? "").toLowerCase().includes(input.toLowerCase());
