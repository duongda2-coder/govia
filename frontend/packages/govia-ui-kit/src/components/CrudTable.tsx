import { useEffect, useMemo, useState } from "react";
import { Table, Card, Popover, Checkbox, Button, Empty } from "antd";
import type { TableProps } from "antd";
import { SettingOutlined, ArrowUpOutlined, ArrowDownOutlined, ReloadOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { StandardToolbar, type StandardToolbarProps } from "./StandardToolbar";

/** `defaultHidden: true` de dinh nghia san 1 cot nhung KHONG hien mac dinh (vd du lieu it dung -
 * username, ngay sinh...) - nguoi dung tu bat len qua nut "Tuy chinh cot" khi can, khong bat buoc
 * phai xoa/them cot moi lan doi y muon xem gi. Chi anh huong lan dau tien (chua co localStorage
 * luu san), sau do theo dung lua chon nguoi dung da luu. */
export type Column<T> = NonNullable<TableProps<T>["columns"]>[number] & { defaultHidden?: boolean };

/** Do rong mac dinh (px) cho 1 cot ellipsis KHONG tu khai bao width - du de doc duoc vai chuc ky
 * tu truoc khi cat "...", hep du de khong choan het bang khi co nhieu cot tu do nhu vay. */
const DEFAULT_ELLIPSIS_WIDTH = 280;

export interface CrudTableProps<T> extends StandardToolbarProps {
  columns: Column<T>[] | undefined;
  dataSource: T[];
  rowKey: string;
  loading?: boolean;
  onSelectionChange?: (keys: React.Key[], rows: T[]) => void;
  pagination?: TableProps<T>["pagination"];
  onChange?: TableProps<T>["onChange"];
  /**
   * Id on dinh, duy nhat cho bang nay trong toan app (vd "documentLibrary",
   * "riskScoring.auditObjectUnit") - dung de luu lai lua chon an/hien + thu tu cot cua nguoi dung
   * vao localStorage, giu nguyen qua cac lan tai lai trang. Bo trong neu khong can nho lai (tuy
   * chinh chi ton tai trong phien lam viec hien tai).
   */
  tableId?: string;
}

interface ColumnPref {
  key: string;
  visible: boolean;
}

function columnKey<T>(col: Column<T>, index: number): string {
  if ("key" in col && col.key != null) return String(col.key);
  if ("dataIndex" in col && col.dataIndex != null) return String(col.dataIndex);
  return `col-${index}`;
}

function columnLabel<T>(col: Column<T>, fallback: string): string {
  return typeof col.title === "string" ? col.title : fallback;
}

function storageKey(tableId: string): string {
  return `govia.crudtable.columns.${tableId}`;
}

function loadPrefs(tableId: string): ColumnPref[] | null {
  try {
    const raw = window.localStorage.getItem(storageKey(tableId));
    return raw ? (JSON.parse(raw) as ColumnPref[]) : null;
  } catch {
    return null;
  }
}

function savePrefs(tableId: string, prefs: ColumnPref[]): void {
  try {
    window.localStorage.setItem(storageKey(tableId), JSON.stringify(prefs));
  } catch {
    // localStorage co the bi chan (vd Safari private mode) - tuy chinh chi mat tac dung nho, khong crash.
  }
}

/** Giu lai thu tu/an-hien nguoi dung da luu cho cac cot van con ton tai, them cot MOI (vd sau khi
 * nang cap man hinh) vao cuoi va mac dinh hien, bo cot khong con trong `columns` nua. */
function mergePrefs(saved: ColumnPref[], defaults: ColumnPref[]): ColumnPref[] {
  const defaultKeys = new Set(defaults.map((d) => d.key));
  const savedKeys = new Set(saved.map((s) => s.key));
  const kept = saved.filter((s) => defaultKeys.has(s.key));
  const added = defaults.filter((d) => !savedKeys.has(d.key));
  return [...kept, ...added];
}

/**
 * Khung man hinh danh sach CHUAN: toolbar hanh dong + bang du lieu + tuy chinh cot (an/hien,
 * doi thu tu). Moi man hinh danh sach cua moi module (Employee, Audit Finding, Vendor...)
 * nen dung component nay de dam bao layout/UX giong nhau toan platform.
 */
export function CrudTable<T extends object>(props: CrudTableProps<T>) {
  const { columns, dataSource, rowKey, loading, onSelectionChange, pagination, onChange, tableId, ...toolbarProps } = props;
  const { t } = useTranslation();

  const allColumns = useMemo(() => columns ?? [], [columns]);
  const columnKeysSignature = useMemo(() => allColumns.map((col, i) => columnKey(col, i)).join("|"), [allColumns]);

  const computeDefaults = (): ColumnPref[] =>
    allColumns.map((col, i) => ({ key: columnKey(col, i), visible: !col.defaultHidden }));

  const [prefs, setPrefs] = useState<ColumnPref[]>(() => {
    const defaults = computeDefaults();
    if (tableId) {
      const saved = loadPrefs(tableId);
      if (saved) return mergePrefs(saved, defaults);
    }
    return defaults;
  });

  useEffect(() => {
    const defaults = computeDefaults();
    if (tableId) {
      const saved = loadPrefs(tableId);
      setPrefs(saved ? mergePrefs(saved, defaults) : defaults);
    } else {
      setPrefs(defaults);
    }
    // Chi tinh lai khi TAP hop cot thay doi (vd chuyen tab sang 1 bang co cot khac) - "columns" la
    // 1 mang moi moi lan render nen khong dung truc tiep lam dependency, tranh vong lap reset lien tuc.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tableId, columnKeysSignature]);

  const updatePrefs = (next: ColumnPref[]) => {
    setPrefs(next);
    if (tableId) savePrefs(tableId, next);
  };

  const columnByKey = useMemo(() => {
    const map = new Map<string, Column<T>>();
    allColumns.forEach((col, i) => map.set(columnKey(col, i), col));
    return map;
  }, [allColumns]);

  const visibleColumns = useMemo(
    () =>
      prefs
        .filter((p) => p.visible)
        .map((p) => columnByKey.get(p.key))
        .filter((c): c is Column<T> => !!c)
        // Mac dinh MOI cot deu ellipsis (1 dong + tooltip khi hover) - tranh gia tri dai lam vo
        // chieu cao dong/xo lech ca bang. Man hinh nao thuc su can hien nguyen van (hiem) thi tu
        // khai bao `ellipsis: false` tren cot do de tat rieng.
        //
        // `ellipsis: true` mot minh KHONG cat chu - antd chi cat khi cot co width co dinh de
        // "cat vao" do; khong co width, cot van tu gian theo noi dung (do scroll x:"max-content"
        // o Table ben duoi cho phep, tranh bug cot 0px) va ca bang bi day rong ra, phai cuon
        // ngang toan bang thay vi cuon rieng trong 1 cot. Nen cot nao chua tu khai bao width thi
        // gan mac dinh DEFAULT_ELLIPSIS_WIDTH de cat that su.
        .map((c) => {
          const ellipsis = c.ellipsis ?? true;
          return { ...c, ellipsis, width: c.width ?? (ellipsis ? DEFAULT_ELLIPSIS_WIDTH : undefined) };
        }),
    [prefs, columnByKey],
  );

  // scroll.x = "max-content" khien rc-table tu chuyen tableLayout ve "auto" (xem rc-table
  // Table.js) - width tren cot chi con la GOI Y, khong phai gioi han cung, nen ellipsis khong cat
  // duoc gi. Dua 1 con so cu the (tong do rong cac cot) thay vi "max-content" de rc-table giu
  // tableLayout "fixed" that su va ep dung do rong da tinh cho tung cot.
  const scrollX = useMemo(() => {
    const total = visibleColumns.reduce((sum, c) => sum + (typeof c.width === "number" ? c.width : 0), 0);
    return total > 0 ? total : "max-content";
  }, [visibleColumns]);

  const toggleVisible = (key: string) => updatePrefs(prefs.map((p) => (p.key === key ? { ...p, visible: !p.visible } : p)));

  const move = (index: number, direction: -1 | 1) => {
    const target = index + direction;
    if (target < 0 || target >= prefs.length) return;
    const next = [...prefs];
    [next[index], next[target]] = [next[target], next[index]];
    updatePrefs(next);
  };

  const reset = () => updatePrefs(computeDefaults());

  const settingsContent = (
    <div style={{ width: 280 }}>
      <div style={{ maxHeight: 320, overflowY: "auto" }}>
        {prefs.map((pref, index) => {
          const col = columnByKey.get(pref.key);
          if (!col) return null;
          return (
            <div key={pref.key} style={{ display: "flex", alignItems: "center", gap: 4, padding: "4px 0" }}>
              <Checkbox checked={pref.visible} onChange={() => toggleVisible(pref.key)} style={{ flex: 1, minWidth: 0 }}>
                {columnLabel(col, pref.key)}
              </Checkbox>
              <Button size="small" type="text" icon={<ArrowUpOutlined />} disabled={index === 0} onClick={() => move(index, -1)} />
              <Button size="small" type="text" icon={<ArrowDownOutlined />} disabled={index === prefs.length - 1} onClick={() => move(index, 1)} />
            </div>
          );
        })}
        {prefs.length === 0 && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={false} />}
      </div>
      <div style={{ marginTop: 8, borderTop: "1px solid #f0f0f0", paddingTop: 8, textAlign: "right" }}>
        <Button size="small" icon={<ReloadOutlined />} onClick={reset}>
          {t("common.resetColumns")}
        </Button>
      </div>
    </div>
  );

  return (
    <Card>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 8 }}>
        <StandardToolbar {...toolbarProps} loading={loading} />
        <Popover content={settingsContent} title={t("common.columnSettings")} trigger="click" placement="bottomRight">
          <Button icon={<SettingOutlined />} title={t("common.columnSettings")} />
        </Popover>
      </div>
      <Table<T>
        style={{ marginTop: 16 }}
        columns={visibleColumns}
        dataSource={dataSource}
        rowKey={rowKey}
        loading={loading}
        pagination={pagination ?? false}
        onChange={onChange}
        // scrollX = tong do rong cac cot (xem tren) thay vi "max-content" - "max-content" khien
        // rc-table tu chuyen tableLayout ve "auto" (width tren cot chi con la goi y), ellipsis se
        // khong cat duoc gi. Voi 1 con so cu the, rc-table giu tableLayout "fixed" that su va ep
        // dung do rong tung cot - an toan vi MOI cot deu da duoc dam bao co width (DEFAULT_ELLIPSIS_WIDTH
        // o tren), nen khong con cot nao "khong ro rong" de bi ep ve 0px (ly do goc "max-content"
        // duoc dung truoc day).
        tableLayout="fixed"
        scroll={{ x: scrollX }}
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
