import { Navigate, Route, Routes } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "./auth/AuthContext";
import { LoginPage } from "./pages/Login/LoginPage";
import { AppLayout } from "./layout/AppLayout";
import { Dashboard } from "./pages/Dashboard";
import { EmployeeListPage } from "./pages/People/EmployeeListPage";
import { PositionCatalogPage } from "./pages/People/PositionCatalogPage";
import { OrganizationUnitListPage } from "./pages/People/OrganizationUnitListPage";
import { RoleListPage } from "./pages/Admin/RoleListPage";
import { AccountListPage } from "./pages/Admin/AccountListPage";
import { ActivityLogPage } from "./pages/Admin/ActivityLogPage";
import { TaskInbox } from "./pages/Workflow/TaskInbox";
import { ProcessInstances } from "./pages/Workflow/ProcessInstances";
import { ApprovalMatrixPage } from "./pages/Workflow/ApprovalMatrixPage";
import { MasterDataGroupPage } from "./pages/Audit/MasterDataGroupPage";
import { DocumentLibraryPage } from "./pages/Audit/DocumentLibrary/DocumentLibraryPage";
import { ControlPointPage } from "./pages/Audit/ControlPoint/ControlPointPage";
import { WorkItemPage } from "./pages/Audit/Plan/WorkItemPage";
import { BranchStaffPage } from "./pages/Audit/Plan/BranchStaffPage";
import { ExceptionTypePage } from "./pages/Audit/Plan/ExceptionTypePage";
import { ProcessStepSummaryPage } from "./pages/Audit/Plan/ProcessStepSummaryPage";
import { ProcessStepDetailPage } from "./pages/Audit/Plan/ProcessStepDetailPage";
import { ExceptionMappingPage } from "./pages/Audit/Plan/ExceptionMappingPage";
import { CmTd1Page } from "./pages/Audit/Plan/Execution/CmTd1Page";
import { CmTd2Page } from "./pages/Audit/Plan/Execution/CmTd2Page";
import { CmNtd1Page } from "./pages/Audit/Plan/Execution/CmNtd1Page";
import { CmNtd2Page } from "./pages/Audit/Plan/Execution/CmNtd2Page";
import { CmNtd3Page } from "./pages/Audit/Plan/Execution/CmNtd3Page";
import { CmNtd4Page } from "./pages/Audit/Plan/Execution/CmNtd4Page";
import { CmNtd6Page } from "./pages/Audit/Plan/Execution/CmNtd6Page";
import { CmNtd7Page } from "./pages/Audit/Plan/Execution/CmNtd7Page";
import { CmNtd8Page } from "./pages/Audit/Plan/Execution/CmNtd8Page";
import { CmNtd9Page } from "./pages/Audit/Plan/Execution/CmNtd9Page";
import { CmNtd10Page } from "./pages/Audit/Plan/Execution/CmNtd10Page";
import { CmNtd11Page } from "./pages/Audit/Plan/Execution/CmNtd11Page";
import { CmNtd12Page } from "./pages/Audit/Plan/Execution/CmNtd12Page";
import { CmNtd13Page } from "./pages/Audit/Plan/Execution/CmNtd13Page";
import { CmNtd14Page } from "./pages/Audit/Plan/Execution/CmNtd14Page";
import { RiskScoringGroupsPage } from "./pages/Audit/RiskScoring/RiskScoringGroupsPage";
import { RiskScoringCriteriaPage } from "./pages/Audit/RiskScoring/RiskScoringCriteriaPage";
import { RiskScoringWeightPage } from "./pages/Audit/RiskScoring/RiskScoringWeightPage";
import { RiskScoringCoefficientMatrixPage } from "./pages/Audit/RiskScoring/RiskScoringCoefficientMatrixPage";
import { RiskScoringUserAssignmentPage } from "./pages/Audit/RiskScoring/RiskScoringUserAssignmentPage";
import { RiskScoringAuditObjectsPage } from "./pages/Audit/RiskScoring/RiskScoringAuditObjectsPage";
import { GroupHOPage } from "./pages/Audit/RiskScoringExec/GroupHOPage";
import { RiskTypeHOPage } from "./pages/Audit/RiskScoringExec/RiskTypeHOPage";
import { RiskCriteriaOtherPage } from "./pages/Audit/RiskScoringExec/RiskCriteriaOtherPage";
import { RiskCriteriaOtherScalePage } from "./pages/Audit/RiskScoringExec/RiskCriteriaOtherScalePage";
import { RiskAssessmentOtherPage } from "./pages/Audit/RiskScoringExec/RiskAssessmentOtherPage";
import { RiskAssessmentOtherRankingPage } from "./pages/Audit/RiskScoringExec/RiskAssessmentOtherRankingPage";
import { RiskAssessmentOtherExpertRankPage } from "./pages/Audit/RiskScoringExec/RiskAssessmentOtherExpertRankPage";
import { RiskCriteriaHsrrPage } from "./pages/Audit/RiskScoringExec/RiskCriteriaHsrrPage";
import { RiskBranchScoreQuantitativePage } from "./pages/Audit/RiskScoringExec/RiskBranchScoreQuantitativePage";
import { RiskBranchScoreQualitativePage } from "./pages/Audit/RiskScoringExec/RiskBranchScoreQualitativePage";
import { RiskBranchScoreCombinedPage } from "./pages/Audit/RiskScoringExec/RiskBranchScoreCombinedPage";
import { RiskBranchScoreExpertRankPage } from "./pages/Audit/RiskScoringExec/RiskBranchScoreExpertRankPage";
import { RiskScoringExecDashboardPage } from "./pages/Audit/RiskScoringExec/RiskScoringExecDashboardPage";
import { AuditFindingPage } from "./pages/Audit/RiskScoringExec/AuditFindingPage";
import { GlobalModalKeyboardShortcuts } from "./components/GlobalModalKeyboardShortcuts";

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
}

function App() {
  const { t } = useTranslation();
  return (
    <>
      <GlobalModalKeyboardShortcuts />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Dashboard />} />
          <Route path="people/employees" element={<EmployeeListPage />} />
          <Route path="people/positions" element={<PositionCatalogPage />} />
          <Route path="people/org-units" element={<OrganizationUnitListPage />} />
          <Route path="admin/roles" element={<RoleListPage />} />
          <Route path="admin/accounts" element={<AccountListPage />} />
          <Route path="admin/activity-log" element={<ActivityLogPage />} />
          <Route path="workflow/tasks" element={<TaskInbox />} />
          <Route path="workflow/instances" element={<ProcessInstances />} />
          <Route path="workflow/approval-matrix" element={<ApprovalMatrixPage />} />
          <Route path="audit/master-data/risk" element={<MasterDataGroupPage group="RISK" title={t("auditMasterData.groups.RISK")} />} />
          <Route path="audit/master-data/general" element={<MasterDataGroupPage group="GENERAL" title={t("auditMasterData.groups.GENERAL")} />} />
          <Route path="audit/master-data/document-library" element={<DocumentLibraryPage />} />
          <Route path="audit/master-data/control-point" element={<ControlPointPage />} />
          <Route path="audit/master-data/department" element={<MasterDataGroupPage group="DEPARTMENT" title={t("auditMasterData.groups.DEPARTMENT")} />} />
          <Route path="audit/master-data/year" element={<MasterDataGroupPage group="YEAR" title={t("auditMasterData.groups.YEAR")} />} />
          <Route path="audit/master-data/business-segment" element={<MasterDataGroupPage group="BUSINESS_SEGMENT" title={t("auditMasterData.groups.BUSINESS_SEGMENT")} />} />
          <Route path="audit/master-data/unit-type" element={<MasterDataGroupPage group="UNIT_TYPE" title={t("auditMasterData.groups.UNIT_TYPE")} />} />
          <Route path="audit/plan/master-data/work-item" element={<WorkItemPage />} />
          <Route path="audit/plan/master-data/branch-staff" element={<BranchStaffPage />} />
          <Route path="audit/plan/master-data/exception-type" element={<ExceptionTypePage />} />
          <Route
            path="audit/plan/master-data/recommendation-type"
            element={<MasterDataGroupPage group="RECOMMENDATION_TYPE" title={t("auditMasterData.groups.RECOMMENDATION_TYPE")} />}
          />
          <Route path="audit/plan/master-data/process-step-summary" element={<ProcessStepSummaryPage />} />
          <Route path="audit/plan/master-data/process-step-detail" element={<ProcessStepDetailPage />} />
          <Route path="audit/plan/master-data/exception-mapping" element={<ExceptionMappingPage />} />
          <Route path="audit/plan/execution/cm-td1" element={<CmTd1Page />} />
          <Route path="audit/plan/execution/cm-td2" element={<CmTd2Page />} />
          <Route path="audit/plan/execution/cm-ntd1" element={<CmNtd1Page />} />
          <Route path="audit/plan/execution/cm-ntd2" element={<CmNtd2Page />} />
          <Route path="audit/plan/execution/cm-ntd3" element={<CmNtd3Page />} />
          <Route path="audit/plan/execution/cm-ntd4" element={<CmNtd4Page />} />
          <Route path="audit/plan/execution/cm-ntd6" element={<CmNtd6Page />} />
          <Route path="audit/plan/execution/cm-ntd7" element={<CmNtd7Page />} />
          <Route path="audit/plan/execution/cm-ntd8" element={<CmNtd8Page />} />
          <Route path="audit/plan/execution/cm-ntd9" element={<CmNtd9Page />} />
          <Route path="audit/plan/execution/cm-ntd10" element={<CmNtd10Page />} />
          <Route path="audit/plan/execution/cm-ntd11" element={<CmNtd11Page />} />
          <Route path="audit/plan/execution/cm-ntd12" element={<CmNtd12Page />} />
          <Route path="audit/plan/execution/cm-ntd13" element={<CmNtd13Page />} />
          <Route path="audit/plan/execution/cm-ntd14" element={<CmNtd14Page />} />
          <Route path="audit/risk-scoring/master-data/groups" element={<RiskScoringGroupsPage />} />
          <Route path="audit/risk-scoring/master-data/criteria" element={<RiskScoringCriteriaPage />} />
          <Route path="audit/risk-scoring/master-data/weight" element={<RiskScoringWeightPage />} />
          <Route path="audit/risk-scoring/master-data/coefficient-matrix" element={<RiskScoringCoefficientMatrixPage />} />
          <Route path="audit/risk-scoring/master-data/user-assignment" element={<RiskScoringUserAssignmentPage />} />
          <Route path="audit/risk-scoring/master-data/audit-objects" element={<RiskScoringAuditObjectsPage />} />
          <Route path="audit/risk-scoring/scoring/dashboard" element={<RiskScoringExecDashboardPage />} />
          <Route path="audit/risk-scoring/scoring/findings" element={<AuditFindingPage />} />
          <Route path="audit/risk-scoring/scoring/group-ho" element={<GroupHOPage />} />
          <Route path="audit/risk-scoring/scoring/risk-type-ho" element={<RiskTypeHOPage />} />
          <Route path="audit/risk-scoring/scoring/criteria-other" element={<RiskCriteriaOtherPage />} />
          <Route path="audit/risk-scoring/scoring/criteria-other-scale" element={<RiskCriteriaOtherScalePage />} />
          <Route path="audit/risk-scoring/scoring/assessment-other" element={<RiskAssessmentOtherPage />} />
          <Route path="audit/risk-scoring/scoring/assessment-other-ranking" element={<RiskAssessmentOtherRankingPage />} />
          <Route path="audit/risk-scoring/scoring/assessment-other-expert-rank" element={<RiskAssessmentOtherExpertRankPage />} />
          <Route path="audit/risk-scoring/scoring/hsrr" element={<RiskCriteriaHsrrPage />} />
          <Route path="audit/risk-scoring/scoring/branch-score-dl" element={<RiskBranchScoreQuantitativePage />} />
          <Route path="audit/risk-scoring/scoring/branch-score-dt" element={<RiskBranchScoreQualitativePage />} />
          <Route path="audit/risk-scoring/scoring/branch-score-all" element={<RiskBranchScoreCombinedPage />} />
          <Route path="audit/risk-scoring/scoring/branch-score-expert-rank" element={<RiskBranchScoreExpertRankPage />} />
          {/* Cac sheet tiep theo cua "2. Cham diem.xlsx" se them 1 route/menu rieng nhu tren, khong phai tab. */}
        </Route>
      </Routes>
    </>
  );
}

export default App;
