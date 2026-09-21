import type { MenuProps } from "antd";
import { AuditOutlined, DashboardOutlined, NodeIndexOutlined, SafetyCertificateOutlined, TeamOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { useAuth } from "../auth/AuthContext";

type MenuItemType = NonNullable<MenuProps["items"]>[number];

/**
 * Route ung voi key cua tung mo-dun trong menu - dung de dieu huong khi bam menu
 * va de xac dinh menu nao dang duoc chon dua theo URL hien tai.
 */
export const MENU_ROUTES: Record<string, string> = {
  dashboard: "/",
  "people-employees": "/people/employees",
  "people-positions": "/people/positions",
  "people-org-units": "/people/org-units",
  "admin-roles": "/admin/roles",
  "admin-accounts": "/admin/accounts",
  "admin-activity-log": "/admin/activity-log",
  "workflow-tasks": "/workflow/tasks",
  "workflow-instances": "/workflow/instances",
  "workflow-approval-matrix": "/workflow/approval-matrix",
  "audit-md-risk": "/audit/master-data/risk",
  "audit-md-general": "/audit/master-data/general",
  "audit-md-document-library": "/audit/master-data/document-library",
  "audit-md-control-point": "/audit/master-data/control-point",
  "audit-md-department": "/audit/master-data/department",
  "audit-md-year": "/audit/master-data/year",
  "audit-md-business-segment": "/audit/master-data/business-segment",
  "audit-md-unit-type": "/audit/master-data/unit-type",
  "audit-md-employee-capability": "/audit/master-data/employee-capability",
  "audit-md-appendix": "/audit/master-data/appendix",
  "audit-plan-md-khkt-scale": "/audit/plan/master-data/khkt-scale",
  "audit-plan-khkt-bp": "/audit/plan/khkt-bp",
  "audit-plan-khkt-th": "/audit/plan/khkt-th",
  "audit-plan-khkt-thang": "/audit/plan/khkt-thang",
  "audit-plan-khkt-dtkh-file": "/audit/plan/khkt-dtkh-file",
  "audit-plan-khns-nam": "/audit/plan/khns-nam",
  "audit-plan-khns-pb": "/audit/plan/khns-pb",
  "audit-plan-khth-transfer": "/audit/plan/khth-transfer",
  "audit-tdkp-assignment": "/audit/tdkp/assignment",
  "audit-tdkp-ceo-all": "/audit/tdkp/ceo-all",
  "audit-tdkp-ceo-kh": "/audit/tdkp/ceo-kh",
  "audit-tdkp-branch": "/audit/tdkp/branch",
  "audit-tdkp-resolution": "/audit/tdkp/resolution",
  "audit-tdkp-unit-recommendation": "/audit/tdkp/unit-recommendation",
  "audit-tdkp-report": "/audit/tdkp/report",
  "audit-plan-md-branch-staff": "/audit/plan/master-data/branch-staff",
  "audit-plan-md-work-item": "/audit/plan/master-data/work-item",
  "audit-plan-md-exception-type": "/audit/plan/master-data/exception-type",
  "audit-plan-md-control-point": "/audit/master-data/control-point",
  "audit-plan-md-recommendation-type": "/audit/plan/master-data/recommendation-type",
  "audit-plan-md-process-step-summary": "/audit/plan/master-data/process-step-summary",
  "audit-plan-md-process-step-detail": "/audit/plan/master-data/process-step-detail",
  "audit-plan-md-exception-mapping": "/audit/plan/master-data/exception-mapping",
  "audit-plan-md-qt-work-item": "/audit/plan/master-data-qt/work-item",
  "audit-plan-md-qt-exception-type": "/audit/plan/master-data-qt/exception-type",
  "audit-plan-md-qt-control-point": "/audit/plan/master-data-qt/control-point",
  "audit-plan-md-qt-process-step-summary": "/audit/plan/master-data-qt/process-step-summary",
  "audit-plan-md-qt-process-step-detail": "/audit/plan/master-data-qt/process-step-detail",
  "audit-plan-md-qt-exception-mapping": "/audit/plan/master-data-qt/exception-mapping",
  "audit-plan-engagement-index": "/audit/plan/engagement",
  "audit-plan-engagement-monitoring-index": "/audit/plan/engagement/monitoring",
  "audit-plan-engagement-supervision-team-index": "/audit/plan/engagement/supervision-team",
  "audit-plan-engagement-process-index": "/audit/plan/engagement/process",
  "audit-plan-engagement-statistics-index": "/audit/plan/engagement/statistics",
  "audit-plan-exec-cm-td1": "/audit/plan/execution/cm-td1",
  "audit-plan-exec-cm-td2": "/audit/plan/execution/cm-td2",
  "audit-plan-exec-cm-ntd1": "/audit/plan/execution/cm-ntd1",
  "audit-plan-exec-cm-ntd2": "/audit/plan/execution/cm-ntd2",
  "audit-plan-exec-cm-ntd3": "/audit/plan/execution/cm-ntd3",
  "audit-plan-exec-cm-ntd4": "/audit/plan/execution/cm-ntd4",
  "audit-plan-exec-cm-ntd6": "/audit/plan/execution/cm-ntd6",
  "audit-plan-exec-cm-ntd7": "/audit/plan/execution/cm-ntd7",
  "audit-plan-exec-cm-ntd8": "/audit/plan/execution/cm-ntd8",
  "audit-plan-exec-cm-ntd9": "/audit/plan/execution/cm-ntd9",
  "audit-plan-exec-cm-ntd10": "/audit/plan/execution/cm-ntd10",
  "audit-plan-exec-cm-ntd11": "/audit/plan/execution/cm-ntd11",
  "audit-plan-exec-cm-ntd12": "/audit/plan/execution/cm-ntd12",
  "audit-plan-exec-cm-ntd13": "/audit/plan/execution/cm-ntd13",
  "audit-plan-exec-cm-ntd14": "/audit/plan/execution/cm-ntd14",
  "audit-plan-exec-cm-ntd15": "/audit/plan/execution/cm-ntd15",
  "audit-plan-exec-cm-ntd16": "/audit/plan/execution/cm-ntd16",
  "audit-plan-exec-work-management-cbkt": "/audit/plan/execution/work-management/cbkt",
  "audit-plan-exec-work-management-thkt": "/audit/plan/execution/work-management/thkt",
  "audit-plan-exec-work-management-ttss": "/audit/plan/execution/work-management/ttss",
  "audit-rs-groups": "/audit/risk-scoring/master-data/groups",
  "audit-rs-criteria": "/audit/risk-scoring/master-data/criteria",
  "audit-rs-weight": "/audit/risk-scoring/master-data/weight",
  "audit-rs-coefficient-matrix": "/audit/risk-scoring/master-data/coefficient-matrix",
  "audit-rs-audit-objects": "/audit/risk-scoring/master-data/audit-objects",
  "audit-rs-user-assignment": "/audit/risk-scoring/master-data/user-assignment",
  "audit-rse-dashboard": "/audit/risk-scoring/scoring/dashboard",
  "audit-rse-findings": "/audit/risk-scoring/scoring/findings",
  "audit-rse-group-ho": "/audit/risk-scoring/scoring/group-ho",
  "audit-rse-risk-type-ho": "/audit/risk-scoring/scoring/risk-type-ho",
  "audit-rse-criteria-other": "/audit/risk-scoring/scoring/criteria-other",
  "audit-rse-criteria-other-scale": "/audit/risk-scoring/scoring/criteria-other-scale",
  "audit-rse-assessment-other": "/audit/risk-scoring/scoring/assessment-other",
  "audit-rse-assessment-other-ranking": "/audit/risk-scoring/scoring/assessment-other-ranking",
  "audit-rse-assessment-other-expert-rank": "/audit/risk-scoring/scoring/assessment-other-expert-rank",
  "audit-rse-hsrr": "/audit/risk-scoring/scoring/hsrr",
  "audit-rse-branch-score-dl": "/audit/risk-scoring/scoring/branch-score-dl",
  "audit-rse-branch-score-dt": "/audit/risk-scoring/scoring/branch-score-dt",
  "audit-rse-branch-score-all": "/audit/risk-scoring/scoring/branch-score-all",
  "audit-rse-branch-score-expert-rank": "/audit/risk-scoring/scoring/branch-score-expert-rank",
};

/** Gan title HTML len nhan menu de trinh duyet tu hien tooltip khi chu bi cat ngan (...) do sider hep. */
function menuLabel(text: string) {
  return <span title={text}>{text}</span>;
}

export interface SearchableScreen {
  key: string;
  label: string;
  groupLabel: string;
  path: string;
}

/**
 * Xay dung menu sidebar + danh sach man hinh co the tim kiem (GlobalSearch tren Dashboard) tu CUNG
 * 1 cho - tranh tinh trang 2 noi khai bao dieu kien quyen roi lech nhau theo thoi gian. "leaf" vua
 * tra ve 1 muc menu vua ghi lai muc do vao searchableScreens - vi no chi duoc goi ben trong bieu
 * thuc "hasPermission && leaf(...)" (nho short-circuit cua &&), leaf() KHONG bao gio chay khi
 * nguoi dung khong co quyen xem man hinh do, nen danh sach tim kiem tu dong dung quyen theo sidebar.
 */
export function useAppMenu(): { moduleMenuItems: MenuProps["items"]; searchableScreens: SearchableScreen[] } {
  const { user, hasPermission } = useAuth();
  const { t } = useTranslation();
  const isSuperAdmin = user?.roles.includes("SUPER_ADMIN") ?? false;

  const searchableScreens: SearchableScreen[] = [];
  const leaf = (key: string, label: string, groupLabel: string): MenuItemType => {
    const path = MENU_ROUTES[key];
    if (path) {
      searchableScreens.push({ key, label, groupLabel, path });
    }
    return { key, label: menuLabel(label) };
  };

  const dropNulls = (items: (MenuItemType | false | null | undefined)[]): MenuItemType[] =>
    items.filter((item): item is MenuItemType => Boolean(item));

  const peopleGroupLabel = t("menu.people");
  const peopleChildren = dropNulls([
    hasPermission("PEOPLE.EMPLOYEE.VIEW") && leaf("people-employees", t("menu.employees"), peopleGroupLabel),
    hasPermission("PEOPLE.POSITION.VIEW") && leaf("people-positions", t("menu.positions"), peopleGroupLabel),
    hasPermission("PEOPLE.ORGUNIT.VIEW") && leaf("people-org-units", t("menu.orgUnits"), peopleGroupLabel),
  ]);

  const workflowGroupLabel = t("menu.workflow");
  const workflowChildren = dropNulls([
    hasPermission("WORKFLOW.TASK.VIEW") && leaf("workflow-tasks", t("menu.workflowTasks"), workflowGroupLabel),
    hasPermission("WORKFLOW.INSTANCE.VIEW") && leaf("workflow-instances", t("menu.workflowInstances"), workflowGroupLabel),
    hasPermission("WORKFLOW.APPROVAL_MATRIX.VIEW") && leaf("workflow-approval-matrix", t("menu.workflowApprovalMatrix"), workflowGroupLabel),
  ]);

  const canViewAuditMasterData = hasPermission("AUDIT.MASTER_DATA.VIEW");
  const canViewAuditPlan = hasPermission("AUDIT.PLAN_MASTER_DATA.VIEW");
  const canViewAuditPlanExecution = hasPermission("AUDIT.PLAN_EXECUTION.VIEW");
  const canViewAuditPlanEngagement = hasPermission("AUDIT.PLAN_ENGAGEMENT.VIEW");
  const canViewAuditSupervisionTeam = hasPermission("AUDIT.SUPERVISION_TEAM.VIEW");
  const canViewAuditPlanEngagementProcess = hasPermission("AUDIT.PLAN_ENGAGEMENT_PROCESS.VIEW");
  const canViewAuditStatistics = hasPermission("AUDIT.STATISTICS.VIEW");
  const canViewRiskScoring = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canViewRiskScoringExec = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const canViewFindings = hasPermission("AUDIT.FINDING.VIEW");

  const auditGroupLabel = t("menu.audit");
  const auditMdGroupLabel = `${auditGroupLabel} / ${t("menu.auditMasterData")} / ${t("menu.auditMdGeneral")}`;
  const auditMdProcessStepGroupLabel = `${auditGroupLabel} / ${t("menu.auditMasterData")} / ${t("menu.auditMdProcessStepGroup")}`;
  const auditMdTtssGroupLabel = `${auditGroupLabel} / ${t("menu.auditMasterData")} / ${t("menu.auditMdTtssGroup")}`;
  const auditPlanMdQtGroupLabel = `${auditGroupLabel} / ${t("menu.auditMasterData")} / ${t("menu.auditPlanMasterDataQt")}`;
  const auditRsCatalogGroupLabel = `${auditGroupLabel} / ${t("menu.riskScoring")} / ${t("menu.riskScoringCatalogGroup")}`;
  const auditRsOtherGroupLabel = `${auditGroupLabel} / ${t("menu.riskScoring")} / ${t("menu.riskScoringOtherGroup")}`;
  const auditRsBranchGroupLabel = `${auditGroupLabel} / ${t("menu.riskScoring")} / ${t("menu.riskScoringBranchGroup")}`;
  const auditPlanExecSampleGroupLabel = `${auditGroupLabel} / ${t("menu.auditPlanExecution")} / ${t("menu.auditPlanExecSampleGroup")}`;
  const auditPlanExecInitGroupLabel = `${auditGroupLabel} / ${t("menu.auditPlanExecution")} / ${t("menu.auditPlanExecInitGroup")}`;
  const auditPlanExecManagementGroupLabel = `${auditGroupLabel} / ${t("menu.auditPlanExecution")} / ${t("menu.auditPlanExecManagementGroup")}`;
  const auditPlanExecSupervisionGroupLabel = `${auditGroupLabel} / ${t("menu.auditPlanExecution")} / ${t("menu.auditPlanExecSupervisionGroup")}`;

  // "Danh muc" (Cap 2): gom Danh muc chung + Danh muc Buoc quy trinh + Danh muc TTSS & KN + Danh muc Kiem toan quy trinh.
  const auditMdGeneralChildren = dropNulls([
    canViewAuditMasterData && leaf("audit-md-general", t("menu.auditMdGeneralCatalogs"), auditMdGroupLabel),
    canViewAuditMasterData && leaf("audit-md-document-library", t("menu.auditMdDocumentLibrary"), auditMdGroupLabel),
    canViewRiskScoring && leaf("audit-rs-audit-objects", t("menu.riskScoringAuditObjects"), auditMdGroupLabel),
    canViewAuditMasterData && leaf("audit-md-department", t("menu.auditMdDepartment"), auditMdGroupLabel),
    canViewAuditMasterData && leaf("audit-md-year", t("menu.auditMdYear"), auditMdGroupLabel),
    canViewAuditMasterData && leaf("audit-md-business-segment", t("menu.auditMdBusinessSegment"), auditMdGroupLabel),
    canViewAuditMasterData && leaf("audit-md-unit-type", t("menu.auditMdUnitType"), auditMdGroupLabel),
    canViewAuditMasterData && leaf("audit-md-employee-capability", t("menu.auditMdEmployeeCapability"), auditMdGroupLabel),
    canViewAuditMasterData && leaf("audit-md-appendix", t("menu.auditMdAppendix"), auditMdGroupLabel),
    canViewAuditPlan && leaf("audit-plan-md-branch-staff", t("menu.auditPlanMdBranchStaff"), auditMdGroupLabel),
    hasPermission("AUDIT.KHKT_SCALE.VIEW") && leaf("audit-plan-md-khkt-scale", t("menu.auditPlanMdKhktScale"), auditMdGroupLabel),
  ]);
  const auditMdProcessStepChildren = canViewAuditPlan
    ? [
        leaf("audit-plan-md-control-point", t("menu.auditMdControlPoint"), auditMdProcessStepGroupLabel),
        leaf("audit-plan-md-work-item", t("menu.auditPlanMdWorkItem"), auditMdProcessStepGroupLabel),
        leaf("audit-plan-md-process-step-summary", t("menu.auditPlanMdProcessStepSummary"), auditMdProcessStepGroupLabel),
        leaf("audit-plan-md-process-step-detail", t("menu.auditPlanMdProcessStepDetail"), auditMdProcessStepGroupLabel),
      ]
    : [];
  const auditMdTtssChildren = canViewAuditPlan
    ? [
        leaf("audit-plan-md-exception-type", t("menu.auditPlanMdExceptionType"), auditMdTtssGroupLabel),
        leaf("audit-plan-md-recommendation-type", t("menu.auditPlanMdRecommendationType"), auditMdTtssGroupLabel),
        leaf("audit-plan-md-exception-mapping", t("menu.auditPlanMdExceptionMapping"), auditMdTtssGroupLabel),
      ]
    : [];
  const auditMdQtChildren = dropNulls([
    hasPermission("AUDIT.CONTROL_POINT_QT.VIEW") &&
      leaf("audit-plan-md-qt-control-point", t("menu.auditPlanMdQtControlPoint"), auditPlanMdQtGroupLabel),
    hasPermission("AUDIT.WORK_ITEM_QT.VIEW") &&
      leaf("audit-plan-md-qt-work-item", t("menu.auditPlanMdQtWorkItem"), auditPlanMdQtGroupLabel),
    hasPermission("AUDIT.PROCESS_STEP_SUMMARY_QT.VIEW") &&
      leaf("audit-plan-md-qt-process-step-summary", t("menu.auditPlanMdQtProcessStepSummary"), auditPlanMdQtGroupLabel),
    hasPermission("AUDIT.PROCESS_STEP_DETAIL_QT.VIEW") &&
      leaf("audit-plan-md-qt-process-step-detail", t("menu.auditPlanMdQtProcessStepDetail"), auditPlanMdQtGroupLabel),
    hasPermission("AUDIT.EXCEPTION_TYPE_QT.VIEW") &&
      leaf("audit-plan-md-qt-exception-type", t("menu.auditPlanMdQtExceptionType"), auditPlanMdQtGroupLabel),
    hasPermission("AUDIT.EXCEPTION_MAPPING_QT.VIEW") &&
      leaf("audit-plan-md-qt-exception-mapping", t("menu.auditPlanMdQtExceptionMapping"), auditPlanMdQtGroupLabel),
  ]);
  const auditMdChildren = dropNulls([
    auditMdGeneralChildren.length > 0 && {
      key: "audit-md-general-group",
      label: menuLabel(t("menu.auditMdGeneral")),
      children: auditMdGeneralChildren,
    },
    auditMdProcessStepChildren.length > 0 && {
      key: "audit-md-process-step-group",
      label: menuLabel(t("menu.auditMdProcessStepGroup")),
      children: auditMdProcessStepChildren,
    },
    auditMdTtssChildren.length > 0 && {
      key: "audit-md-ttss-group",
      label: menuLabel(t("menu.auditMdTtssGroup")),
      children: auditMdTtssChildren,
    },
    auditMdQtChildren.length > 0 && {
      key: "audit-plan-master-data-qt",
      label: menuLabel(t("menu.auditPlanMasterDataQt")),
      children: auditMdQtChildren,
    },
  ]);

  // "Cham diem rui ro" (Cap 2): gom Danh muc cham diem + Cham diem rui ro khac CN + Cham diem rui ro CN.
  const auditRsCatalogChildren = dropNulls([
    canViewAuditMasterData && leaf("audit-md-risk", t("menu.auditMdRisk"), auditRsCatalogGroupLabel),
    canViewRiskScoring && leaf("audit-rs-groups", t("menu.riskScoringGroups"), auditRsCatalogGroupLabel),
    canViewRiskScoring && leaf("audit-rs-criteria", t("menu.riskScoringCriteria"), auditRsCatalogGroupLabel),
    canViewRiskScoring && leaf("audit-rs-weight", t("menu.riskScoringWeight"), auditRsCatalogGroupLabel),
    canViewRiskScoring && leaf("audit-rs-coefficient-matrix", t("menu.riskScoringCoefficientMatrix"), auditRsCatalogGroupLabel),
    canViewRiskScoring && leaf("audit-rs-user-assignment", t("menu.riskScoringUserAssignment"), auditRsCatalogGroupLabel),
  ]);
  const auditRsOtherChildren = canViewRiskScoringExec
    ? [
        leaf("audit-rse-group-ho", t("menu.riskScoringExecGroupHO"), auditRsOtherGroupLabel),
        leaf("audit-rse-risk-type-ho", t("menu.riskScoringExecRiskTypeHO"), auditRsOtherGroupLabel),
        leaf("audit-rse-criteria-other", t("menu.riskScoringExecCriteriaOther"), auditRsOtherGroupLabel),
        leaf("audit-rse-criteria-other-scale", t("menu.riskScoringExecCriteriaOtherScale"), auditRsOtherGroupLabel),
        leaf("audit-rse-assessment-other", t("menu.riskScoringExecAssessmentOther"), auditRsOtherGroupLabel),
        leaf("audit-rse-assessment-other-ranking", t("menu.riskScoringExecAssessmentOtherRanking"), auditRsOtherGroupLabel),
        leaf("audit-rse-assessment-other-expert-rank", t("menu.riskScoringExecAssessmentOtherExpertRank"), auditRsOtherGroupLabel),
      ]
    : [];
  const auditRsBranchChildren = canViewRiskScoringExec
    ? dropNulls([
        leaf("audit-rse-dashboard", t("menu.riskScoringExecDashboard"), auditRsBranchGroupLabel),
        canViewFindings && leaf("audit-rse-findings", t("menu.riskScoringExecFindings"), auditRsBranchGroupLabel),
        leaf("audit-rse-hsrr", t("menu.riskScoringExecHsrr"), auditRsBranchGroupLabel),
        leaf("audit-rse-branch-score-dl", t("menu.riskScoringExecBranchScoreQuantitative"), auditRsBranchGroupLabel),
        leaf("audit-rse-branch-score-dt", t("menu.riskScoringExecBranchScoreQualitative"), auditRsBranchGroupLabel),
        leaf("audit-rse-branch-score-all", t("menu.riskScoringExecBranchScoreCombined"), auditRsBranchGroupLabel),
        leaf("audit-rse-branch-score-expert-rank", t("menu.riskScoringExecBranchScoreExpertRank"), auditRsBranchGroupLabel),
      ])
    : [];
  const auditRsChildren = dropNulls([
    auditRsCatalogChildren.length > 0 && {
      key: "audit-rs-catalog-group",
      label: menuLabel(t("menu.riskScoringCatalogGroup")),
      children: auditRsCatalogChildren,
    },
    auditRsOtherChildren.length > 0 && {
      key: "audit-rs-other-group",
      label: menuLabel(t("menu.riskScoringOtherGroup")),
      children: auditRsOtherChildren,
    },
    auditRsBranchChildren.length > 0 && {
      key: "audit-rs-branch-group",
      label: menuLabel(t("menu.riskScoringBranchGroup")),
      children: auditRsBranchChildren,
    },
  ]);

  // "Thuc hien kiem toan" (Cap 2): gom Quan ly mau chon + Khoi tao + Quan ly cuoc KT + To giam sat.
  const auditExecSampleChildren = canViewAuditPlanExecution
    ? dropNulls([
        // 17 sheet ZTC_CM_TD1/TD2/NTD1-NTD16 - moi sheet 1 muc menu/route rieng.
        hasPermission("AUDIT.CM_TD1.VIEW") && leaf("audit-plan-exec-cm-td1", t("menu.auditPlanExecCmTd1"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_TD2.VIEW") && leaf("audit-plan-exec-cm-td2", t("menu.auditPlanExecCmTd2"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_NTD1.VIEW") && leaf("audit-plan-exec-cm-ntd1", t("menu.auditPlanExecCmNtd1"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_NTD2.VIEW") && leaf("audit-plan-exec-cm-ntd2", t("menu.auditPlanExecCmNtd2"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_NTD3.VIEW") && leaf("audit-plan-exec-cm-ntd3", t("menu.auditPlanExecCmNtd3"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_NTD4.VIEW") && leaf("audit-plan-exec-cm-ntd4", t("menu.auditPlanExecCmNtd4"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_NTD6.VIEW") && leaf("audit-plan-exec-cm-ntd6", t("menu.auditPlanExecCmNtd6"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_NTD7.VIEW") && leaf("audit-plan-exec-cm-ntd7", t("menu.auditPlanExecCmNtd7"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_NTD8.VIEW") && leaf("audit-plan-exec-cm-ntd8", t("menu.auditPlanExecCmNtd8"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_NTD9.VIEW") && leaf("audit-plan-exec-cm-ntd9", t("menu.auditPlanExecCmNtd9"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_NTD10.VIEW") && leaf("audit-plan-exec-cm-ntd10", t("menu.auditPlanExecCmNtd10"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_NTD11.VIEW") && leaf("audit-plan-exec-cm-ntd11", t("menu.auditPlanExecCmNtd11"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_NTD12.VIEW") && leaf("audit-plan-exec-cm-ntd12", t("menu.auditPlanExecCmNtd12"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_NTD13.VIEW") && leaf("audit-plan-exec-cm-ntd13", t("menu.auditPlanExecCmNtd13"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_NTD14.VIEW") && leaf("audit-plan-exec-cm-ntd14", t("menu.auditPlanExecCmNtd14"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_NTD15.VIEW") && leaf("audit-plan-exec-cm-ntd15", t("menu.auditPlanExecCmNtd15"), auditPlanExecSampleGroupLabel),
        hasPermission("AUDIT.CM_NTD16.VIEW") && leaf("audit-plan-exec-cm-ntd16", t("menu.auditPlanExecCmNtd16"), auditPlanExecSampleGroupLabel),
      ])
    : [];
  const auditExecInitChildren = dropNulls([
    canViewAuditPlanEngagement && leaf("audit-plan-engagement-index", t("menu.auditPlanEngagementIndex"), auditPlanExecInitGroupLabel),
    canViewAuditPlanEngagementProcess &&
      leaf("audit-plan-engagement-process-index", t("menu.auditPlanEngagementProcessIndex"), auditPlanExecInitGroupLabel),
  ]);
  const auditExecManagementChildren = dropNulls([
    canViewAuditPlanEngagement &&
      leaf("audit-plan-engagement-monitoring-index", t("menu.auditPlanEngagementMonitoringIndex"), auditPlanExecManagementGroupLabel),
    canViewAuditPlanExecution &&
      hasPermission("AUDIT.WORK_MANAGEMENT.VIEW") &&
      leaf("audit-plan-exec-work-management-cbkt", t("menu.auditPlanExecWorkManagementCbkt"), auditPlanExecManagementGroupLabel),
    canViewAuditPlanExecution &&
      hasPermission("AUDIT.WORK_MANAGEMENT.VIEW") &&
      leaf("audit-plan-exec-work-management-thkt", t("menu.auditPlanExecWorkManagementThkt"), auditPlanExecManagementGroupLabel),
    canViewAuditPlanExecution &&
      hasPermission("AUDIT.TTSS.VIEW") &&
      leaf("audit-plan-exec-work-management-ttss", t("menu.auditPlanExecWorkManagementTtss"), auditPlanExecManagementGroupLabel),
    canViewAuditStatistics &&
      leaf("audit-plan-engagement-statistics-index", t("menu.auditPlanEngagementStatisticsIndex"), auditPlanExecManagementGroupLabel),
  ]);
  const auditExecSupervisionChildren = dropNulls([
    canViewAuditSupervisionTeam &&
      leaf("audit-plan-engagement-supervision-team-index", t("menu.auditPlanEngagementSupervisionTeamIndex"), auditPlanExecSupervisionGroupLabel),
  ]);
  const auditExecChildren = dropNulls([
    auditExecSampleChildren.length > 0 && {
      key: "audit-plan-exec-sample-group",
      label: menuLabel(t("menu.auditPlanExecSampleGroup")),
      children: auditExecSampleChildren,
    },
    auditExecInitChildren.length > 0 && {
      key: "audit-plan-exec-init-group",
      label: menuLabel(t("menu.auditPlanExecInitGroup")),
      children: auditExecInitChildren,
    },
    auditExecManagementChildren.length > 0 && {
      key: "audit-plan-exec-management-group",
      label: menuLabel(t("menu.auditPlanExecManagementGroup")),
      children: auditExecManagementChildren,
    },
    auditExecSupervisionChildren.length > 0 && {
      key: "audit-plan-exec-supervision-group",
      label: menuLabel(t("menu.auditPlanExecSupervisionGroup")),
      children: auditExecSupervisionChildren,
    },
  ]);

  const auditTdkpGroupLabel = t("menu.auditTdkp");
  const auditTdkpChildren = dropNulls([
    hasPermission("AUDIT.TDKP_PC.VIEW") && leaf("audit-tdkp-assignment", t("menu.auditTdkpAssignment"), auditTdkpGroupLabel),
    hasPermission("AUDIT.TDKP_CEO_ALL.VIEW") && leaf("audit-tdkp-ceo-all", t("menu.auditTdkpCeoAll"), auditTdkpGroupLabel),
    hasPermission("AUDIT.TDKP_CEO_KH.VIEW") && leaf("audit-tdkp-ceo-kh", t("menu.auditTdkpCeoKh"), auditTdkpGroupLabel),
    hasPermission("AUDIT.TDKP_CN.VIEW") && leaf("audit-tdkp-branch", t("menu.auditTdkpBranch"), auditTdkpGroupLabel),
    hasPermission("AUDIT.TDKP_NQ.VIEW") && leaf("audit-tdkp-resolution", t("menu.auditTdkpResolution"), auditTdkpGroupLabel),
    hasPermission("AUDIT.TDKP_KTNB.VIEW") && leaf("audit-tdkp-unit-recommendation", t("menu.auditTdkpUnitRecommendation"), auditTdkpGroupLabel),
    hasPermission("AUDIT.TDKP_BC.VIEW") && leaf("audit-tdkp-report", t("menu.auditTdkpReport"), auditTdkpGroupLabel),
  ]);

  const auditChildren = dropNulls([
    auditMdChildren.length > 0 && { key: "audit-master-data", label: menuLabel(t("menu.auditMasterData")), children: auditMdChildren },
    auditRsChildren.length > 0 && { key: "audit-risk-scoring", label: menuLabel(t("menu.riskScoring")), children: auditRsChildren },
    auditExecChildren.length > 0 && {
      key: "audit-plan-execution",
      label: menuLabel(t("menu.auditPlanExecution")),
      children: auditExecChildren,
    },
    (hasPermission("AUDIT.KHKT_BP.VIEW") ||
      hasPermission("AUDIT.KHKT_TH.VIEW") ||
      hasPermission("AUDIT.KHKT_THANG.VIEW") ||
      hasPermission("AUDIT.KHKT_DTKH_FILE.VIEW") ||
      hasPermission("AUDIT.KHNS_NAM.VIEW") ||
      hasPermission("AUDIT.KHTH_TRANSFER.VIEW")) && {
      key: "audit-plan-kehoach",
      label: menuLabel(t("menu.auditPlanKeHoach")),
      children: dropNulls([
        hasPermission("AUDIT.KHKT_BP.VIEW") &&
          leaf("audit-plan-khkt-bp", t("menu.auditPlanKhktBp"), `${auditGroupLabel} / ${t("menu.auditPlanKeHoach")}`),
        hasPermission("AUDIT.KHKT_TH.VIEW") &&
          leaf("audit-plan-khkt-th", t("menu.auditPlanKhktTh"), `${auditGroupLabel} / ${t("menu.auditPlanKeHoach")}`),
        hasPermission("AUDIT.KHKT_THANG.VIEW") &&
          leaf("audit-plan-khkt-thang", t("menu.auditPlanKhktThang"), `${auditGroupLabel} / ${t("menu.auditPlanKeHoach")}`),
        hasPermission("AUDIT.KHKT_DTKH_FILE.VIEW") &&
          leaf("audit-plan-khkt-dtkh-file", t("menu.auditPlanKhktDtkhFile"), `${auditGroupLabel} / ${t("menu.auditPlanKeHoach")}`),
        hasPermission("AUDIT.KHNS_NAM.VIEW") &&
          leaf("audit-plan-khns-nam", t("menu.auditPlanKhnsNam"), `${auditGroupLabel} / ${t("menu.auditPlanKeHoach")}`),
        hasPermission("AUDIT.KHNS_NAM.VIEW") &&
          leaf("audit-plan-khns-pb", t("menu.auditPlanKhnsPb"), `${auditGroupLabel} / ${t("menu.auditPlanKeHoach")}`),
        hasPermission("AUDIT.KHTH_TRANSFER.VIEW") &&
          leaf("audit-plan-khth-transfer", t("menu.auditPlanKhthTransfer"), `${auditGroupLabel} / ${t("menu.auditPlanKeHoach")}`),
      ]),
    },
    auditTdkpChildren.length > 0 && { key: "audit-tdkp", label: menuLabel(t("menu.auditTdkp")), children: auditTdkpChildren },
  ]);

  const adminGroupLabel = t("menu.admin");

  const moduleMenuItems: MenuProps["items"] = dropNulls([
    { key: "dashboard", icon: <DashboardOutlined />, label: menuLabel(t("menu.dashboard")) },
    peopleChildren.length > 0 && { key: "people", icon: <TeamOutlined />, label: menuLabel(peopleGroupLabel), children: peopleChildren },
    workflowChildren.length > 0 && { key: "workflow", icon: <NodeIndexOutlined />, label: menuLabel(workflowGroupLabel), children: workflowChildren },
    auditChildren.length > 0 && { key: "audit", icon: <AuditOutlined />, label: menuLabel(auditGroupLabel), children: auditChildren },
    isSuperAdmin && {
      key: "admin",
      icon: <SafetyCertificateOutlined />,
      label: menuLabel(adminGroupLabel),
      children: [
        leaf("admin-roles", t("menu.roles"), adminGroupLabel),
        leaf("admin-accounts", t("menu.accounts"), adminGroupLabel),
        leaf("admin-activity-log", t("menu.activityLog"), adminGroupLabel),
      ],
    },
  ]);

  return { moduleMenuItems, searchableScreens };
}
