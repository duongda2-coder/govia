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
import { TaskInbox } from "./pages/Workflow/TaskInbox";
import { ProcessInstances } from "./pages/Workflow/ProcessInstances";
import { ApprovalMatrixPage } from "./pages/Workflow/ApprovalMatrixPage";
import { MasterDataGroupPage } from "./pages/Audit/MasterDataGroupPage";
import { DocumentLibraryPage } from "./pages/Audit/DocumentLibrary/DocumentLibraryPage";
import { ControlPointPage } from "./pages/Audit/ControlPoint/ControlPointPage";
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
          <Route path="audit/risk-scoring/master-data/groups" element={<RiskScoringGroupsPage />} />
          <Route path="audit/risk-scoring/master-data/criteria" element={<RiskScoringCriteriaPage />} />
          <Route path="audit/risk-scoring/master-data/weight" element={<RiskScoringWeightPage />} />
          <Route path="audit/risk-scoring/master-data/coefficient-matrix" element={<RiskScoringCoefficientMatrixPage />} />
          <Route path="audit/risk-scoring/master-data/user-assignment" element={<RiskScoringUserAssignmentPage />} />
          <Route path="audit/risk-scoring/master-data/audit-objects" element={<RiskScoringAuditObjectsPage />} />
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
          {/* Cac sheet tiep theo cua "2. Cham diem.xlsx" se them 1 route/menu rieng nhu tren, khong phai tab. */}
        </Route>
      </Routes>
    </>
  );
}

export default App;
