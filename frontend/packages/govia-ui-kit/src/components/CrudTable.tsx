import { Table, Card } from "antd";
import type { TableProps } from "antd";
import { StandardToolbar, type StandardToolbarProps } from "./StandardToolbar";

export interface CrudTableProps<T> extends StandardToolbarProps {
  columns: TableProps<T>["columns"];
  dataSource: T[];
  rowKey: string;
  loading?: boolean;
  onSelectionChange?: (keys: React.Key[], rows: T[]) => void;
  pagination?: TableProps<T>["pagination"];
  onChange?: TableProps<T>["onChange"];
}

/**
 * Khung man hinh danh sach CHUAN: toolbar hanh dong + bang du lieu.
 * Moi man hinh danh sach cua moi module (Employee, Audit Finding, Vendor...)
 * nen dung component nay de dam bao layout/UX giong nhau toan platform.
 */
export function CrudTable<T extends object>(props: CrudTableProps<T>) {
  const { columns, dataSource, rowKey, loading, onSelectionChange, pagination, onChange, ...toolbarProps } = props;

  return (
    <Card>
      <StandardToolbar {...toolbarProps} loading={loading} />
      <Table<T>
        style={{ marginTop: 16 }}
        columns={columns}
        dataSource={dataSource}
        rowKey={rowKey}
        loading={loading}
        pagination={pagination ?? false}
        onChange={onChange}
        rowSelection={
          onSelectionChange
            ? {
                onChange: (keys, rows) => onSelectionChange(keys, rows),
              }
            : undefined
        }
      />
    </Card>
  );
}
