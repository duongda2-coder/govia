import { useCallback, useRef, useState } from "react";
import type { TableProps } from "antd";

export interface ServerTableQuery {
  page: number;
  size: number;
  sort?: string;
  [field: string]: string | number | undefined;
}

/**
 * State + handler DUNG CHUNG cho moi man hinh danh sach server-side (phan trang, loc tung cot,
 * sap xep tung cot) cua TAT CA module GOVIA. Ket hop voi getSearchColumnProps/getSelectFilterColumnProps
 * ben duoi de moi man hinh co UI/UX loc-sap xep giong het nhau, khong tu viet lai state rieng.
 *
 * sortFieldMap: anh xa dataIndex cua cot -> duong dan sort backend hieu, dung khi ten cot khac
 * ten field thuc su tren server (vd cot "orgUnitName" hien thi nhung backend sort theo "orgUnit.name").
 */
export function useServerTable<T extends object>(sortFieldMap: Record<string, string> = {}) {
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [sort, setSort] = useState<string | undefined>();

  const filtersSignature = useRef("");
  const sortSignature = useRef("");

  const handleChange: NonNullable<TableProps<T>["onChange"]> = useCallback(
    (pagination, tableFilters, sorter) => {
      const nextFilters: Record<string, string> = {};
      Object.entries(tableFilters).forEach(([key, values]) => {
        if (values && values.length > 0) {
          nextFilters[key] = String(values[0]);
        }
      });

      const activeSorter = Array.isArray(sorter) ? sorter[0] : sorter;
      const nextSort =
        activeSorter?.order && typeof activeSorter.field === "string"
          ? `${sortFieldMap[activeSorter.field] ?? activeSorter.field},${activeSorter.order === "ascend" ? "asc" : "desc"}`
          : undefined;

      const nextFiltersSig = JSON.stringify(nextFilters);
      const nextSortSig = nextSort ?? "";
      const filterOrSortChanged = nextFiltersSig !== filtersSignature.current || nextSortSig !== sortSignature.current;
      filtersSignature.current = nextFiltersSig;
      sortSignature.current = nextSortSig;

      setFilters(nextFilters);
      setSort(nextSort);
      if (pagination.pageSize) {
        setPageSize(pagination.pageSize);
      }
      setPage(filterOrSortChanged ? 1 : (pagination.current ?? 1));
    },
    [sortFieldMap],
  );

  const query: ServerTableQuery = { page: page - 1, size: pageSize, sort, ...filters };

  const pagination = (total: number): TableProps<T>["pagination"] => ({
    current: page,
    pageSize,
    total,
    showSizeChanger: true,
  });

  return { query, filters, sort, page, pageSize, handleChange, pagination };
}
