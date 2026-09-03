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
  "audit-plan-md-branch-staff": "/audit/plan/master-data/branch-staff",
  "audit-plan-md-work-item": "/audit/plan/master-data/work-item",
  "audit-plan-md-exception-type": "/audit/plan/master-data/exception-type",
  "audit-plan-md-control-point": "/audit/master-data/control-point",
  "audit-plan-md-recommendation-type": "/audit/plan/master-data/recommendation-type",
  "audit-plan-md-process-step-summary": "/audit/plan/master-data/process-step-summary",
  "audit-plan-md-process-step-detail": "/audit/plan/master-data/process-step-detail",
  "audit-plan-md-exception-mapping": "/audit/plan/master-data/exception-mapping",
  "audit-plan-engagement-index": "/audit/plan/engagement",
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
  const canViewRiskScoring = hasPermission("AUDIT.RISK_SCORING.VIEW");
  const canViewRiskScoringExec = hasPermission("AUDIT.RISK_SCORING_EXEC.VIEW");
  const canViewFindings = hasPermission("AUDIT.FINDING.VIEW");

  const auditGroupLabel = t("menu.audit");
  const auditMdGroupLabel = `${auditGroupLabel} / ${t("menu.auditMasterData")}`;
  const auditPlanMdGroupLabel = `${auditGroupLabel} / ${t("menu.auditPlan")} / ${t("menu.auditPlanMasterData")}`;
  const auditPlanEngagementGroupLabel = `${auditGroupLabel} / ${t("menu.auditPlan")} / ${t("menu.auditPlanEngagement")}`;
  const auditPlanExecGroupLabel = `${auditGroupLabel} / ${t("menu.auditPlan")} / ${t("menu.auditPlanExecution")}`;
  const auditRsGroupLabel = `${auditGroupLabel} / ${t("menu.riskScoring")} / ${t("menu.riskScoringMasterData")}`;
  const auditRseGroupLabel = `${auditGroupLabel} / ${t("menu.riskScoring")} / ${t("menu.riskScoringExec")}`;

  const auditChildren = dropNulls([
    canViewAuditMasterData && {
      key: "audit-master-data",
      label: menuLabel(t("menu.auditMasterData")),
      children: [
        leaf("audit-md-risk", t("menu.auditMdRisk"), auditMdGroupLabel),
        leaf("audit-md-general", t("menu.auditMdGeneral"), auditMdGroupLabel),
        leaf("audit-md-document-library", t("menu.auditMdDocumentLibrary"), auditMdGroupLabel),
        leaf("audit-md-control-point", t("menu.auditMdControlPoint"), auditMdGroupLabel),
        leaf("audit-md-department", t("menu.auditMdDepartment"), auditMdGroupLabel),
        leaf("audit-md-year", t("menu.auditMdYear"), auditMdGroupLabel),
        leaf("audit-md-business-segment", t("menu.auditMdBusinessSegment"), auditMdGroupLabel),
        leaf("audit-md-unit-type", t("menu.auditMdUnitType"), auditMdGroupLabel),
        leaf("audit-md-employee-capability", t("menu.auditMdEmployeeCapability"), auditMdGroupLabel),
        leaf("audit-md-appendix", t("menu.auditMdAppendix"), auditMdGroupLabel),
      ],
    },
    (canViewRiskScoring || canViewRiskScoringExec) && {
      key: "audit-risk-scoring",
      label: menuLabel(t("menu.riskScoring")),
      children: dropNulls([
        canViewRiskScoring && {
          key: "audit-rs-master-data",
          label: menuLabel(t("menu.riskScoringMasterData")),
          children: [
            leaf("audit-rs-groups", t("menu.riskScoringGroups"), auditRsGroupLabel),
            leaf("audit-rs-criteria", t("menu.riskScoringCriteria"), auditRsGroupLabel),
            leaf("audit-rs-weight", t("menu.riskScoringWeight"), auditRsGroupLabel),
            leaf("audit-rs-coefficient-matrix", t("menu.riskScoringCoefficientMatrix"), auditRsGroupLabel),
            leaf("audit-rs-user-assignment", t("menu.riskScoringUserAssignment"), auditRsGroupLabel),
            leaf("audit-rs-audit-objects", t("menu.riskScoringAuditObjects"), auditRsGroupLabel),
          ],
        },
        canViewRiskScoringExec && {
          key: "audit-rse-master-data",
          label: menuLabel(t("menu.riskScoringExec")),
          children: dropNulls([
            // Them tab moi vao day khi xu ly them sheet cua "2. Cham diem.xlsx" - moi sheet la 1 muc
            // menu/route rieng (giong Master Data CDRR), khong phai tab trong 1 trang.
            leaf("audit-rse-dashboard", t("menu.riskScoringExecDashboard"), auditRseGroupLabel),
            canViewFindings && leaf("audit-rse-findings", t("menu.riskScoringExecFindings"), auditRseGroupLabel),
            leaf("audit-rse-group-ho", t("menu.riskScoringExecGroupHO"), auditRseGroupLabel),
            leaf("audit-rse-risk-type-ho", t("menu.riskScoringExecRiskTypeHO"), auditRseGroupLabel),
            leaf("audit-rse-criteria-other", t("menu.riskScoringExecCriteriaOther"), auditRseGroupLabel),
            leaf("audit-rse-criteria-other-scale", t("menu.riskScoringExecCriteriaOtherScale"), auditRseGroupLabel),
            leaf("audit-rse-assessment-other", t("menu.riskScoringExecAssessmentOther"), auditRseGroupLabel),
            leaf("audit-rse-assessment-other-ranking", t("menu.riskScoringExecAssessmentOtherRanking"), auditRseGroupLabel),
            leaf("audit-rse-assessment-other-expert-rank", t("menu.riskScoringExecAssessmentOtherExpertRank"), auditRseGroupLabel),
            leaf("audit-rse-hsrr", t("menu.riskScoringExecHsrr"), auditRseGroupLabel),
            leaf("audit-rse-branch-score-dl", t("menu.riskScoringExecBranchScoreQuantitative"), auditRseGroupLabel),
            leaf("audit-rse-branch-score-dt", t("menu.riskScoringExecBranchScoreQualitative"), auditRseGroupLabel),
            leaf("audit-rse-branch-score-all", t("menu.riskScoringExecBranchScoreCombined"), auditRseGroupLabel),
            leaf("audit-rse-branch-score-expert-rank", t("menu.riskScoringExecBranchScoreExpertRank"), auditRseGroupLabel),
          ]),
        },
      ]),
    },
    canViewAuditPlan && {
      key: "audit-plan",
      label: menuLabel(t("menu.auditPlan")),
      children: dropNulls([
        {
          key: "audit-plan-master-data",
          label: menuLabel(t("menu.auditPlanMasterData")),
          children: [
            leaf("audit-plan-md-branch-staff", t("menu.auditPlanMdBranchStaff"), auditPlanMdGroupLabel),
            leaf("audit-plan-md-work-item", t("menu.auditPlanMdWorkItem"), auditPlanMdGroupLabel),
            leaf("audit-plan-md-exception-type", t("menu.auditPlanMdExceptionType"), auditPlanMdGroupLabel),
            leaf("audit-plan-md-control-point", t("menu.auditMdControlPoint"), auditPlanMdGroupLabel),
            leaf("audit-plan-md-recommendation-type", t("menu.auditPlanMdRecommendationType"), auditPlanMdGroupLabel),
            leaf("audit-plan-md-process-step-summary", t("menu.auditPlanMdProcessStepSummary"), auditPlanMdGroupLabel),
            leaf("audit-plan-md-process-step-detail", t("menu.auditPlanMdProcessStepDetail"), auditPlanMdGroupLabel),
            leaf("audit-plan-md-exception-mapping", t("menu.auditPlanMdExceptionMapping"), auditPlanMdGroupLabel),
          ],
        },
        canViewAuditPlanEngagement && {
          key: "audit-plan-engagement",
          label: menuLabel(t("menu.auditPlanEngagement")),
          children: [leaf("audit-plan-engagement-index", t("menu.auditPlanEngagementIndex"), auditPlanEngagementGroupLabel)],
        },
        canViewAuditPlanExecution && {
          key: "audit-plan-execution",
          label: menuLabel(t("menu.auditPlanExecution")),
          children: dropNulls([
            // 15 sheet ZTC_CM_TD1/TD2/NTD1-NTD14 - moi sheet 1 muc menu/route rieng (giong Danh muc).
            hasPermission("AUDIT.CM_TD1.VIEW") && leaf("audit-plan-exec-cm-td1", t("menu.auditPlanExecCmTd1"), auditPlanExecGroupLabel),
            hasPermission("AUDIT.CM_TD2.VIEW") && leaf("audit-plan-exec-cm-td2", t("menu.auditPlanExecCmTd2"), auditPlanExecGroupLabel),
            hasPermission("AUDIT.CM_NTD1.VIEW") && leaf("audit-plan-exec-cm-ntd1", t("menu.auditPlanExecCmNtd1"), auditPlanExecGroupLabel),
            hasPermission("AUDIT.CM_NTD2.VIEW") && leaf("audit-plan-exec-cm-ntd2", t("menu.auditPlanExecCmNtd2"), auditPlanExecGroupLabel),
            hasPermission("AUDIT.CM_NTD3.VIEW") && leaf("audit-plan-exec-cm-ntd3", t("menu.auditPlanExecCmNtd3"), auditPlanExecGroupLabel),
            hasPermission("AUDIT.CM_NTD4.VIEW") && leaf("audit-plan-exec-cm-ntd4", t("menu.auditPlanExecCmNtd4"), auditPlanExecGroupLabel),
            hasPermission("AUDIT.CM_NTD6.VIEW") && leaf("audit-plan-exec-cm-ntd6", t("menu.auditPlanExecCmNtd6"), auditPlanExecGroupLabel),
            hasPermission("AUDIT.CM_NTD7.VIEW") && leaf("audit-plan-exec-cm-ntd7", t("menu.auditPlanExecCmNtd7"), auditPlanExecGroupLabel),
            hasPermission("AUDIT.CM_NTD8.VIEW") && leaf("audit-plan-exec-cm-ntd8", t("menu.auditPlanExecCmNtd8"), auditPlanExecGroupLabel),
            hasPermission("AUDIT.CM_NTD9.VIEW") && leaf("audit-plan-exec-cm-ntd9", t("menu.auditPlanExecCmNtd9"), auditPlanExecGroupLabel),
            hasPermission("AUDIT.CM_NTD10.VIEW") && leaf("audit-plan-exec-cm-ntd10", t("menu.auditPlanExecCmNtd10"), auditPlanExecGroupLabel),
            hasPermission("AUDIT.CM_NTD11.VIEW") && leaf("audit-plan-exec-cm-ntd11", t("menu.auditPlanExecCmNtd11"), auditPlanExecGroupLabel),
            hasPermission("AUDIT.CM_NTD12.VIEW") && leaf("audit-plan-exec-cm-ntd12", t("menu.auditPlanExecCmNtd12"), auditPlanExecGroupLabel),
            hasPermission("AUDIT.CM_NTD13.VIEW") && leaf("audit-plan-exec-cm-ntd13", t("menu.auditPlanExecCmNtd13"), auditPlanExecGroupLabel),
            hasPermission("AUDIT.CM_NTD14.VIEW") && leaf("audit-plan-exec-cm-ntd14", t("menu.auditPlanExecCmNtd14"), auditPlanExecGroupLabel),
          ]),
        },
      ]),
    },
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
