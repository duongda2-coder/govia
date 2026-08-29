import { useMemo, useState } from "react";
import { AutoComplete, Input, Typography } from "antd";
import { SearchOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import type { SearchableScreen } from "../layout/useAppMenu";

/** Bo dau tieng Viet + ve chu thuong de nguoi dung go khong dau van tim duoc ("danh muc" -> "Danh mục"). */
const DIACRITIC_MARKS_RE = /[̀-ͯ]/g;

function normalize(value: string): string {
  return value
    .toLowerCase()
    .normalize("NFD")
    .replace(DIACRITIC_MARKS_RE, "")
    .replace(/đ/g, "d");
}

interface GlobalSearchProps {
  screens: SearchableScreen[];
}

/** O tim kiem nhanh tren Dashboard - go 1 phan ten man hinh, bam ket qua se dieu huong thang toi do. */
export function GlobalSearch({ screens }: GlobalSearchProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [query, setQuery] = useState("");

  const results = useMemo(() => {
    const needle = normalize(query.trim());
    if (!needle) return [];
    return screens
      .map((screen) => ({ screen, index: normalize(screen.label).indexOf(needle) }))
      .filter(({ screen, index }) => index >= 0 || normalize(screen.groupLabel).includes(needle))
      .sort((a, b) => {
        const rank = (i: number) => (i === 0 ? 0 : i > 0 ? 1 : 2);
        return rank(a.index) - rank(b.index);
      })
      .slice(0, 20)
      .map(({ screen }) => screen);
  }, [screens, query]);

  const options = results.map((screen) => ({
    value: screen.key,
    label: (
      <div style={{ padding: "2px 0" }}>
        <div>{screen.label}</div>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          {screen.groupLabel}
        </Typography.Text>
      </div>
    ),
  }));

  const goTo = (key: string) => {
    const screen = screens.find((s) => s.key === key);
    if (!screen) return;
    navigate(screen.path);
    setQuery("");
  };

  return (
    <AutoComplete
      style={{ width: "100%", maxWidth: 480 }}
      options={options}
      value={query}
      filterOption={false}
      onSearch={setQuery}
      onSelect={goTo}
      onKeyDown={(e) => {
        if (e.key === "Enter" && results.length > 0) {
          goTo(results[0].key);
        }
      }}
      notFoundContent={query.trim() ? t("dashboard.search.noResults") : null}
    >
      <Input size="large" prefix={<SearchOutlined />} placeholder={t("dashboard.search.placeholder")} allowClear />
    </AutoComplete>
  );
}
