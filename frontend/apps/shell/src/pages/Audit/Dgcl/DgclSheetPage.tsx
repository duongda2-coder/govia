import { useEffect, useState } from "react";
import { Button, Col, Row, Tabs, Typography } from "antd";
import { useTranslation } from "react-i18next";
import type { AuditEngagementItem } from "../../../api/auditEngagement";
import { DGCL_APPENDICES, getDgclCapability, type DgclAppendix, type DgclCapability, type DgclSubjectRow } from "../../../api/auditDgcl";
import { DgclAppendixSheet } from "./DgclAppendixSheet";

interface Props {
  engagement: AuditEngagementItem;
  subject: DgclSubjectRow;
  onBack: () => void;
}

/** Nut "Đánh giá" (sheet logic dong 45-47): 3 lua chon tuong ung 3 sheet PL 01A / PL 01B / PL 01F. */
export function DgclSheetPage({ engagement, subject, onBack }: Props) {
  const { t } = useTranslation();
  const [active, setActive] = useState<DgclAppendix>("PL01A");
  const [capability, setCapability] = useState<DgclCapability>({ canEvaluate: false, canControl: false });

  useEffect(() => {
    getDgclCapability()
      .then(setCapability)
      .catch(() => setCapability({ canEvaluate: false, canControl: false }));
  }, []);

  const name = subject.team ? t("auditDgcl.teamRow") : subject.employeeName;

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 8 }}>
        <Col>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {t("auditDgcl.sheetTitle", { name, code: engagement.code })}
          </Typography.Title>
          <Typography.Text type="secondary">
            {[subject.segmentCodes && `${t("auditDgcl.columns.segmentCodes")}: ${subject.segmentCodes}`, subject.role && `${t("auditDgcl.columns.role")}: ${subject.role}`]
              .filter(Boolean)
              .join(" · ")}
          </Typography.Text>
        </Col>
        <Col>
          <Button onClick={onBack}>{t("common.back")}</Button>
        </Col>
      </Row>

      <Tabs
        activeKey={active}
        onChange={(k) => setActive(k as DgclAppendix)}
        items={DGCL_APPENDICES.map((appendix) => ({
          key: appendix,
          label: t(`auditDgcl.appendix.${appendix}`),
          children: (
            <DgclAppendixSheet
              engagementId={engagement.id}
              subjectKey={subject.subjectKey}
              subjectSegments={subject.segmentCodes}
              team={subject.team}
              appendix={appendix}
              capability={capability}
              active={active === appendix}
            />
          ),
        }))}
      />
    </div>
  );
}
