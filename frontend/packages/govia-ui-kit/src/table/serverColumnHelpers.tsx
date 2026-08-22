import { useRef } from "react";
import { Button, Input, Radio, Space } from "antd";
import type { InputRef } from "antd";
import type { ColumnType } from "antd/es/table";
import type { FilterDropdownProps } from "antd/es/table/interface";
import { SearchOutlined } from "@ant-design/icons";

export interface FilterActionLabels {
  confirmText: string;
  resetText: string;
}

/**
 * DLL loc/sap xep theo tung cot DUNG CHUNG cho moi bang danh sach cua moi module GOVIA.
 * Ket hop voi useServerTable: filter/sort thuc su chay o server (qua query params), Table
 * chi hien thi UI - "onFilter: () => true" la bat buoc de antd KHONG tu loc lai o client
 * (khong co no se an het du lieu server da tra ve dung).
 *
 * Dung filterDropdown TU VIET (khong dung "filters" mac dinh cua antd) cho ca 2 loai cot,
 * vi nut Reset/OK mac dinh cua antd doc text tu ConfigProvider locale - trong app nay locale
 * doi dong theo ngon ngu nguoi dung chon nen bi rong chu; tu viet dam bao luon hien dung nhan.
 */
export function useSearchColumn<T>() {
  const inputRef = useRef<InputRef>(null);

  function getSearchColumnProps(dataIndex: string, activeValue: string | undefined, labels: FilterActionLabels): Partial<ColumnType<T>> {
    return {
      filterDropdown: ({ setSelectedKeys, selectedKeys, confirm, clearFilters }: FilterDropdownProps) => (
        <div style={{ padding: 8 }} onKeyDown={(e) => e.stopPropagation()}>
          <Input
            key={dataIndex}
            ref={inputRef}
            placeholder={labels.confirmText}
            value={selectedKeys[0] as string}
            onChange={(e) => setSelectedKeys(e.target.value ? [e.target.value] : [])}
            onPressEnter={() => confirm()}
            style={{ marginBottom: 8, display: "block", width: 200 }}
          />
          <Space>
            <Button type="primary" size="small" icon={<SearchOutlined />} onClick={() => confirm()}>
              {labels.confirmText}
            </Button>
            <Button
              size="small"
              onClick={() => {
                clearFilters?.();
                confirm();
              }}
            >
              {labels.resetText}
            </Button>
          </Space>
        </div>
      ),
      filterIcon: (filtered: boolean) => <SearchOutlined style={{ color: filtered ? "#2563eb" : undefined }} />,
      filteredValue: activeValue ? [activeValue] : null,
      onFilter: () => true,
    };
  }

  return { getSearchColumnProps };
}

/**
 * Bien the tim/sap xep CLIENT-SIDE - dung cho bang khong phan trang server (danh sach master-data
 * nho, tai het 1 lan nhu Chuc danh, Don vi to chuc). Cung UI voi useSearchColumn nhung loc/sap xep
 * That ngay tren du lieu da co san trong trinh duyet (khong goi lai API), khong can quan ly state filter.
 */
export function useClientSearchColumn<T extends object>() {
  const inputRef = useRef<InputRef>(null);

  function getSearchColumnProps(dataIndex: keyof T & string, labels: FilterActionLabels): ColumnType<T> {
    const getValue = (record: T): unknown => (record as Record<string, unknown>)[dataIndex];
    return {
      dataIndex,
      filterDropdown: ({ setSelectedKeys, selectedKeys, confirm, clearFilters }: FilterDropdownProps) => (
        <div style={{ padding: 8 }} onKeyDown={(e) => e.stopPropagation()}>
          <Input
            key={dataIndex}
            ref={inputRef}
            placeholder={labels.confirmText}
            value={selectedKeys[0] as string}
            onChange={(e) => setSelectedKeys(e.target.value ? [e.target.value] : [])}
            onPressEnter={() => confirm()}
            style={{ marginBottom: 8, display: "block", width: 200 }}
          />
          <Space>
            <Button type="primary" size="small" icon={<SearchOutlined />} onClick={() => confirm()}>
              {labels.confirmText}
            </Button>
            <Button
              size="small"
              onClick={() => {
                clearFilters?.();
                confirm();
              }}
            >
              {labels.resetText}
            </Button>
          </Space>
        </div>
      ),
      filterIcon: (filtered: boolean) => <SearchOutlined style={{ color: filtered ? "#2563eb" : undefined }} />,
      onFilter: (value, record) =>
        String(getValue(record) ?? "")
          .toLowerCase()
          .includes(String(value).toLowerCase()),
      sorter: (a, b) => String(getValue(a) ?? "").localeCompare(String(getValue(b) ?? "")),
    };
  }

  return { getSearchColumnProps };
}

export interface SelectFilterOption {
  text: React.ReactNode;
  value: string;
}

/** Cot loc theo danh sach gia tri co dinh (enum: trang thai, loai...) - chon 1 gia tri qua radio. */
export function useSelectFilterColumn<T>() {
  function getSelectFilterColumnProps(
    dataIndex: string,
    options: SelectFilterOption[],
    activeValue: string | undefined,
    labels: FilterActionLabels,
  ): Partial<ColumnType<T>> {
    return {
      filterDropdown: ({ setSelectedKeys, selectedKeys, confirm, clearFilters }: FilterDropdownProps) => (
        <div style={{ padding: 8, minWidth: 160 }}>
          <Radio.Group
            key={dataIndex}
            value={selectedKeys[0]}
            onChange={(e) => setSelectedKeys([e.target.value])}
            style={{ display: "flex", flexDirection: "column", gap: 4, marginBottom: 8 }}
          >
            {options.map((o) => (
              <Radio key={o.value} value={o.value}>
                {o.text}
              </Radio>
            ))}
          </Radio.Group>
          <Space>
            <Button type="primary" size="small" onClick={() => confirm()}>
              {labels.confirmText}
            </Button>
            <Button
              size="small"
              onClick={() => {
                setSelectedKeys([]);
                clearFilters?.();
                confirm();
              }}
            >
              {labels.resetText}
            </Button>
          </Space>
        </div>
      ),
      filteredValue: activeValue ? [activeValue] : null,
      onFilter: () => true,
    };
  }

  return { getSelectFilterColumnProps };
}
