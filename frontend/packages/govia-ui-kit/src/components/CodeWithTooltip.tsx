import { Tooltip, Typography } from "antd";

export interface CodeWithTooltipProps {
  code: string | null | undefined;
  name: string | null | undefined;
}

/**
 * Hien thi MA (ngan gon) trong cot bang, de con chuot vao hien Tooltip ten day du.
 * Dung chung cho moi cot tham chieu master-data (don vi, chuc danh, quan ly...) cua moi module GOVIA.
 */
export function CodeWithTooltip({ code, name }: CodeWithTooltipProps) {
  if (!code) {
    return <span>-</span>;
  }
  return (
    <Tooltip title={name || undefined}>
      <Typography.Text style={{ cursor: name ? "help" : undefined }}>{code}</Typography.Text>
    </Tooltip>
  );
}
