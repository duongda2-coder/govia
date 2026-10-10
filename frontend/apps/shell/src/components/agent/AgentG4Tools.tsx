import { useEffect, useState } from "react";
import { Alert, App, Button, Form, Input, Select, Space, Table, Tag, Typography, Upload } from "antd";
import { CopyOutlined, RobotOutlined, UploadOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import {
  agentG4DraftApi,
  type DgclScoreDraftItem,
  type DgclScoreDraftResult,
  type ImportCatalog,
  type ImportCheckIssue,
  type ImportCheckResult,
} from "../../api/agent";
import { DGCL_APPENDICES, listDgclEngagements, listDgclSubjects, type DgclAppendix, type DgclSubjectRow } from "../../api/auditDgcl";
import type { AuditEngagementItem } from "../../api/auditEngagement";
import { agentErrorKey } from "./agentErrors";

const VALUE_COLOR: Record<string, string> = {
  COMPLIANT: "success",
  NOT_APPLIES: "success",
  NON_COMPLIANT: "error",
  APPLIES: "error",
  NEED_REVIEW: "default",
  NOT_SET: "default",
};

function useCopy() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  return async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      message.success(t("agent.panel.copied"));
    } catch {
      message.error(t("agent.panel.copyFailed"));
    }
  };
}

/**
 * "Gợi ý chấm ĐGCL" (A6, G4): AI goi y tung tieu chi cua 1 phieu kem can cu ho so doan. CHI DE THAM KHAO - khong dien
 * vao phieu, khong luu; nguoi cham tu tich/nhap, Lưu va Xác nhận tren man hinh Danh gia chat luong.
 */
export function DgclScoreTool() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const copy = useCopy();
  const [engagements, setEngagements] = useState<AuditEngagementItem[]>([]);
  const [subjects, setSubjects] = useState<DgclSubjectRow[]>([]);
  const [engagementId, setEngagementId] = useState<string>();
  const [subjectKey, setSubjectKey] = useState<string>();
  const [appendix, setAppendix] = useState<DgclAppendix>("PL01A");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<DgclScoreDraftResult | null>(null);

  useEffect(() => {
    listDgclEngagements().then(setEngagements).catch(() => setEngagements([]));
  }, []);

  useEffect(() => {
    setSubjects([]);
    setSubjectKey(undefined);
    if (!engagementId) return;
    listDgclSubjects(engagementId).then(setSubjects).catch(() => setSubjects([]));
  }, [engagementId]);

  const run = async () => {
    if (!engagementId || !subjectKey) return;
    setLoading(true);
    setResult(null);
    try {
      setResult(await agentG4DraftApi.dgclScore(engagementId, subjectKey, appendix));
    } catch (err) {
      message.error(t(agentErrorKey(err)));
    } finally {
      setLoading(false);
    }
  };

  const label = (v: string) => t(`agent.dgcl.value${v}`);
  const asText = (r: DgclScoreDraftResult) =>
    [
      `${r.appendix} - ${r.subject} (${r.engagementCode})`,
      ...r.items.map(
        (i) => `${i.stt ?? ""} [${i.key}] ${label(i.suggestion)}${i.violationCount ? ` (${i.violationCount})` : ""}${i.reason ? `: ${i.reason}` : ""}`,
      ),
      r.overall ?? "",
    ].join("\n");

  return (
    <>
      <Form layout="vertical">
        <Form.Item label={t("agent.panel.engagement")} required>
          <Select
            showSearch
            optionFilterProp="label"
            value={engagementId}
            onChange={setEngagementId}
            placeholder={t("agent.dgcl.engagementPlaceholder")}
            options={engagements.map((e) => ({ value: e.id, label: `${e.code} - ${e.auditObjectUnitName ?? ""}` }))}
          />
        </Form.Item>
        <Form.Item label={t("agent.dgcl.subject")} required>
          <Select
            value={subjectKey}
            onChange={setSubjectKey}
            disabled={!engagementId}
            options={subjects.map((s) => ({
              value: s.subjectKey,
              label: s.team ? t("agent.dgcl.team") : `${s.role ?? ""} ${s.employeeName}${s.employeeCode ? ` - ${s.employeeCode}` : ""}`,
            }))}
          />
        </Form.Item>
        <Form.Item label={t("agent.dgcl.appendix")}>
          <Select<DgclAppendix> value={appendix} onChange={setAppendix} options={DGCL_APPENDICES.map((a) => ({ value: a, label: a }))} />
        </Form.Item>
        <Button type="primary" icon={<RobotOutlined />} loading={loading} disabled={!engagementId || !subjectKey} onClick={run}>
          {t("agent.dgcl.run")}
        </Button>
      </Form>
      {result && (
        <>
          <Alert
            type="info"
            showIcon
            message={t("agent.dgcl.hint")}
            description={t("agent.dgcl.summary", {
              positive: result.suggestedPositive,
              negative: result.suggestedNegative,
              review: result.needReview,
              ratio: result.suggestedRatio == null ? "-" : `${result.suggestedRatio}%`,
              current: result.currentScore ?? "-",
            })}
          />
          {!result.grounded && <Alert type="warning" showIcon message={t("agent.dgcl.notGrounded")} />}
          {result.downgraded > 0 && <Alert type="warning" showIcon message={t("agent.dgcl.downgraded", { count: result.downgraded })} />}
          {!result.sheetSaved && <Alert type="warning" showIcon message={t("agent.dgcl.notSaved")} />}
          {result.truncated && <Alert type="warning" showIcon message={t("agent.dgcl.truncated", { count: result.criteriaCount })} />}
          {result.overall && <Typography.Paragraph style={{ marginBottom: 0 }}>{result.overall}</Typography.Paragraph>}
          <ul style={{ margin: 0, paddingLeft: 18, fontSize: 12, color: "rgba(0,0,0,0.45)" }}>
            {result.dossierFacts.map((f, i) => (
              <li key={i}>{f}</li>
            ))}
          </ul>
          <Table<DgclScoreDraftItem>
            size="small"
            rowKey="key"
            pagination={{ pageSize: 20, size: "small" }}
            scroll={{ x: "max-content" }}
            dataSource={result.items}
            columns={[
              { title: t("agent.dgcl.colStt"), dataIndex: "stt", width: 50, render: (v) => v ?? "-" },
              {
                title: t("agent.dgcl.colCriterion"),
                dataIndex: "content",
                width: 220,
                render: (v: string, row) => (
                  <Typography.Paragraph ellipsis={{ rows: 3, tooltip: v }} style={{ marginBottom: 0, fontSize: 12 }}>
                    {v}
                    {row.reason && (
                      <>
                        <br />
                        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                          {row.reason}
                        </Typography.Text>
                      </>
                    )}
                  </Typography.Paragraph>
                ),
              },
              {
                title: t("agent.dgcl.colSuggestion"),
                width: 120,
                render: (_, row) => (
                  <Space direction="vertical" size={2}>
                    <Tag color={VALUE_COLOR[row.suggestion]}>
                      {label(row.suggestion)}
                      {row.violationCount ? ` (${row.violationCount})` : ""}
                    </Tag>
                    {row.differsFromCurrent && <Tag color="warning">{t("agent.dgcl.differs")}</Tag>}
                  </Space>
                ),
              },
              {
                title: t("agent.dgcl.colCurrent"),
                width: 110,
                render: (_, row) => <Tag color={VALUE_COLOR[row.current]}>{label(row.current)}</Tag>,
              },
            ]}
          />
          <Button icon={<CopyOutlined />} onClick={() => copy(asText(result))} style={{ alignSelf: "flex-start" }}>
            {t("agent.panel.copy")}
          </Button>
        </>
      )}
    </>
  );
}

const IMPORT_CATALOGS: ImportCatalog[] = [
  "control_point",
  "control_point_qt",
  "work_item",
  "work_item_qt",
  "exception_type",
  "exception_type_qt",
  "exception_mapping",
  "exception_mapping_qt",
  "process_step",
  "process_step_qt",
];

/**
 * "Kiểm tra file trước khi import" (A7, G4): so file voi dung mau Import cua danh muc va du lieu dang co. Khong import,
 * khong luu file, khong goi AI - nguoi dung sua file roi tu bam Import tren man hinh danh muc.
 */
export function ImportCheckTool({ defaultCatalog }: { defaultCatalog: ImportCatalog }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const [catalog, setCatalog] = useState<ImportCatalog>(defaultCatalog);
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ImportCheckResult | null>(null);

  useEffect(() => {
    setCatalog(defaultCatalog);
  }, [defaultCatalog]);

  const run = async () => {
    if (!file) return;
    setLoading(true);
    setResult(null);
    try {
      setResult(await agentG4DraftApi.importCheck(catalog, file));
    } catch (err) {
      message.error(t(agentErrorKey(err)));
    } finally {
      setLoading(false);
    }
  };

  const issueColumns = [
    { title: t("agent.importCheck.row"), dataIndex: "row", width: 60 },
    { title: t("agent.importCheck.message"), dataIndex: "message" },
  ];

  return (
    <>
      <Form layout="vertical">
        <Form.Item label={t("agent.importCheck.catalog")}>
          <Select<ImportCatalog>
            value={catalog}
            onChange={setCatalog}
            options={IMPORT_CATALOGS.map((c) => ({ value: c, label: t(`agent.importCheck.catalog_${c}`) }))}
          />
        </Form.Item>
        <Form.Item label={t("agent.importCheck.file")} extra={t("agent.importCheck.fileHint")}>
          <Upload
            accept=".xlsx,.xls"
            maxCount={1}
            fileList={file ? [{ uid: "import-check", name: file.name, status: "done" }] : []}
            beforeUpload={(f) => {
              setFile(f);
              setResult(null);
              return false;
            }}
            onRemove={() => {
              setFile(null);
              setResult(null);
            }}
          >
            <Button icon={<UploadOutlined />}>{t("agent.importCheck.choose")}</Button>
          </Upload>
        </Form.Item>
        <Button type="primary" loading={loading} disabled={!file} onClick={run}>
          {t("agent.importCheck.run")}
        </Button>
      </Form>
      {result && (
        <>
          <Alert
            type={result.readyToImport ? (result.warnings.length ? "warning" : "success") : "error"}
            showIcon
            message={t(result.readyToImport ? "agent.importCheck.ready" : "agent.importCheck.notReady")}
            description={t("agent.importCheck.summary", {
              total: result.totalRows,
              ok: result.okRows,
              errors: result.errorRows,
              warnings: result.warningRows,
            })}
          />
          {result.missingHeaders.length > 0 && (
            <div>
              <Typography.Text strong>{t("agent.importCheck.missingHeaders")}</Typography.Text>{" "}
              {result.missingHeaders.map((h) => (
                <Tag key={h} color="error">
                  {h}
                </Tag>
              ))}
            </div>
          )}
          {result.unknownHeaders.length > 0 && (
            <div>
              <Typography.Text strong>{t("agent.importCheck.unknownHeaders")}</Typography.Text>{" "}
              {result.unknownHeaders.map((h) => (
                <Tag key={h} color="warning">
                  {h}
                </Tag>
              ))}
            </div>
          )}
          {result.headerHints.map((h, i) => (
            <Alert key={i} type="warning" showIcon message={h} />
          ))}
          {result.codeColumn && (
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {t("agent.importCheck.keyColumns", { code: result.codeColumn, year: result.yearColumn ?? "-", name: result.nameColumn ?? "-" })}
            </Typography.Text>
          )}
          {result.errors.length > 0 && (
            <Table<ImportCheckIssue>
              size="small"
              rowKey={(r, i) => `e${r.row}-${i}`}
              title={() => t("agent.importCheck.errors")}
              pagination={{ pageSize: 10, size: "small" }}
              dataSource={result.errors}
              columns={issueColumns}
            />
          )}
          {result.warnings.length > 0 && (
            <Table<ImportCheckIssue>
              size="small"
              rowKey={(r, i) => `w${r.row}-${i}`}
              title={() => t("agent.importCheck.warnings")}
              pagination={{ pageSize: 10, size: "small" }}
              dataSource={result.warnings}
              columns={issueColumns}
            />
          )}
          {result.truncated && <Typography.Text type="secondary">{t("agent.importCheck.truncated")}</Typography.Text>}
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {t("agent.importCheck.expectedHeaders")}
          </Typography.Text>
          <Input.TextArea readOnly autoSize={{ minRows: 1, maxRows: 3 }} value={result.expectedHeaders.join(" | ")} />
        </>
      )}
    </>
  );
}
