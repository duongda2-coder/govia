import { useEffect, useState } from "react";
import { App, Alert, Button, Checkbox, Col, Input, Row, Space, Spin, Typography } from "antd";
import { useTranslation } from "react-i18next";
import dayjs from "dayjs";
import {
  getMySupervisionEvaluation,
  saveMySupervisionEvaluation,
  type AuditSupervisionEvaluationItem,
} from "../../../../api/auditSupervisionTeam";
import type { AuditEngagementMonitoringItem } from "../../../../api/auditEngagementMonitoring";

export interface AuditSupervisionEvaluationPageProps {
  engagement: Pick<AuditEngagementMonitoringItem, "id" | "code">;
  onBack: () => void;
}

/** "To giam sat thuc hien danh gia" (sheet "To giam sat" cua Tao CKT (4).xlsx) - chi hien thi VA
 * cho sua dong danh gia cua CHINH nguoi dang dang nhap (khong the tich ho/tich cheo thanh vien
 * khac trong to giam sat); evaluatedAt do server tu set = thoi diem luu gan nhat. */
export function AuditSupervisionEvaluationPage({ engagement, onBack }: AuditSupervisionEvaluationPageProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();

  const [loading, setLoading] = useState(true);
  const [notMember, setNotMember] = useState(false);
  const [data, setData] = useState<AuditSupervisionEvaluationItem | null>(null);
  const [progress, setProgress] = useState(false);
  const [contentAssured, setContentAssured] = useState(false);
  const [qualityAssured, setQualityAssured] = useState(false);
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getMySupervisionEvaluation(engagement.id)
      .then((row) => {
        if (cancelled) return;
        setData(row);
        setProgress(row.progress);
        setContentAssured(row.contentAssured);
        setQualityAssured(row.qualityAssured);
        setNote(row.note ?? "");
      })
      .catch(() => {
        if (!cancelled) setNotMember(true);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [engagement.id]);

  const handleSave = async () => {
    setSaving(true);
    try {
      const saved = await saveMySupervisionEvaluation(engagement.id, { progress, contentAssured, qualityAssured, note: note || null });
      setData(saved);
      message.success(t("auditSupervisionTeam.messages.saveEvaluationSuccess"));
    } catch {
      message.error(t("auditSupervisionTeam.messages.saveEvaluationError"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {t("auditSupervisionTeam.evaluationTitle", { code: engagement.code })}
          </Typography.Title>
        </Col>
        <Col>
          <Button onClick={onBack}>{t("common.back")}</Button>
        </Col>
      </Row>

      {loading && <Spin />}

      {!loading && notMember && <Alert type="warning" showIcon message={t("auditSupervisionTeam.notMemberWarning")} />}

      {!loading && !notMember && data && (
        <Space direction="vertical" size="middle" style={{ width: "100%", maxWidth: 560 }}>
          <div>
            <Typography.Text type="secondary">{t("auditSupervisionTeam.columns.employeeCode")}: </Typography.Text>
            <Typography.Text strong>{data.employeeCode}</Typography.Text>
            {" · "}
            <Typography.Text type="secondary">{t("auditSupervisionTeam.columns.employeeName")}: </Typography.Text>
            <Typography.Text strong>{data.employeeName}</Typography.Text>
            {" · "}
            <Typography.Text type="secondary">{t("auditSupervisionTeam.columns.username")}: </Typography.Text>
            <Typography.Text strong>{data.username ?? "-"}</Typography.Text>
          </div>
          <div>
            <Typography.Text type="secondary">{t("auditSupervisionTeam.columns.evaluatedAt")}: </Typography.Text>
            <Typography.Text>{data.evaluatedAt ? dayjs(data.evaluatedAt).format("DD.MM.YYYY HH:mm") : "-"}</Typography.Text>
          </div>
          <Checkbox checked={progress} onChange={(e) => setProgress(e.target.checked)}>
            {t("auditSupervisionTeam.columns.progress")}
          </Checkbox>
          <Checkbox checked={contentAssured} onChange={(e) => setContentAssured(e.target.checked)}>
            {t("auditSupervisionTeam.columns.contentAssured")}
          </Checkbox>
          <Checkbox checked={qualityAssured} onChange={(e) => setQualityAssured(e.target.checked)}>
            {t("auditSupervisionTeam.columns.qualityAssured")}
          </Checkbox>
          <div>
            <Typography.Text type="secondary">{t("auditSupervisionTeam.columns.note")}</Typography.Text>
            <Input.TextArea value={note} onChange={(e) => setNote(e.target.value)} rows={3} maxLength={1000} />
          </div>
          <Button type="primary" loading={saving} onClick={handleSave}>
            {t("common.save")}
          </Button>
        </Space>
      )}
    </div>
  );
}
