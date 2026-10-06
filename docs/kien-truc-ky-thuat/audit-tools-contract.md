# Audit Tools — data contract cho AI Agent

Tài liệu này là **nguồn duy nhất** mô tả các "tool" mà AI Agent hỗ trợ module
Chấm điểm rủi ro được phép gọi. Toàn bộ tool đều là **GET, read-only** — agent
không có bất kỳ tool nào ghi/sửa dữ liệu, và mọi field trong output đều đọc
trực tiếp từ database qua service đã có sẵn (không có field nào do agent tự
suy diễn hay bịa ra).

Bề mặt API duy nhất: `AuditToolsController`
(`backend/govia-identity/src/main/java/com/govia/audit/tools/controller/AuditToolsController.java`),
gọi qua `AuditToolsService`
(`backend/govia-identity/src/main/java/com/govia/audit/tools/service/AuditToolsService.java`).
Service này **không đụng repository trực tiếp** — mỗi method chỉ compose lại
service nghiệp vụ đã có (không viết lại logic tính điểm/xếp hạng).

Khi sinh tool definition cho LLM function-calling (OpenAI/Anthropic), dùng
đúng tên tool + input schema ở đây làm `parameters`, và mô tả ở cột "Mô tả"
làm `description`.

---

## 1. `get_branch_risk`

Lấy điểm và mức xếp loại rủi ro tổng hợp của 1 chi nhánh trong 1 năm.

- **Endpoint**: `GET /api/audit/tools/branch-risk?branchCode&year`
- **Permission**: `AUDIT.RISK_SCORING_EXEC.VIEW`
- **Nguồn**: `RiskBranchScoreCombinedService.list(year)` (đã dùng cho màn "Kết quả chấm điểm tổng hợp")

**Input**

| Field | Type | Bắt buộc |
|---|---|---|
| `branchCode` | string | có |
| `year` | integer | có |

**Output** (`RiskBranchScoreCombinedRowResponse`, `null` nếu chi nhánh chưa có điểm cho năm đó)

| Field | Type | Ghi chú |
|---|---|---|
| `branchCode` | string | |
| `branchName` | string \| null | |
| `year` | integer | |
| `totalScore` | number | điểm quy đổi tổng hợp (định tính + định lượng theo tỷ trọng) |
| `rankLabel` | string \| null | xếp loại — chuỗi động lấy từ danh mục `RiskScoreRank`, KHÔNG phải enum cố định |
| `scoresByBusinessLineCode` | object (map mã nghiệp vụ → number \| null) | |

> **`null` khác với lỗi**: nếu response là `null`, nghĩa là chi nhánh đó chưa
> được chấm điểm cho năm được hỏi — agent phải trình bày là "chưa có dữ
> liệu chấm điểm", không được đoán một con số thay thế.

---

## 2. `get_branch_details`

Thông tin hồ sơ của 1 chi nhánh/đơn vị (thành lập, nhân sự, chức năng, phát hiện trọng yếu).

- **Endpoint**: `GET /api/audit/tools/branch-details?branchCode`
- **Permission**: `AUDIT.RISK_SCORING_EXEC.VIEW`
- **Nguồn**: `AuditObjectUnitService.list()` (danh mục "Đối tượng kiểm toán")

**Input**: `branchCode` (string, bắt buộc)

**Output** (`AuditObjectUnitResponse`, `null` nếu không tìm thấy mã)

| Field | Type |
|---|---|
| `code`, `name`, `unitType` | string |
| `establishedDate`, `restructureDate`, `infoUpdatedDate` | date \| null |
| `totalStaff`, `leaderCount`, `staffCount`, `rankValue` | integer \| null |
| `operatingRegulation`, `mainFunction`, `keyFindings` | string \| null (text tự do, KHÔNG phải danh sách phát hiện có cấu trúc — xem tool 9) |
| `active` | boolean |

---

## 3. `get_risk_breakdown`

Chi tiết điểm rủi ro của 1 chi nhánh/năm theo từng nghiệp vụ / từng chỉ tiêu định lượng / từng nhóm cấp 2 định tính.

- **Endpoint**: `GET /api/audit/tools/risk-breakdown?branchCode&year`
- **Permission**: `AUDIT.RISK_SCORING_EXEC.VIEW`
- **Nguồn**: ghép `RiskBranchScoreCombinedService` + `RiskBranchScoreQuantitativeService` + `RiskBranchScoreQualitativeService`

**Input**: `branchCode` (string), `year` (integer) — cả hai bắt buộc

**Output** (`RiskBreakdownResponse`, `null` nếu không có điểm tổng hợp cho chi nhánh/năm đó)

| Field | Type | Ghi chú |
|---|---|---|
| `branchCode`, `branchName`, `year`, `totalScore`, `rankLabel` | như tool 1 | |
| `scoresByBusinessLine` | object | điểm theo từng nghiệp vụ (giống tool 1) |
| `scoresByCriteriaQuantitative` | object \| null | điểm theo **từng chỉ tiêu định lượng** (mã chỉ tiêu → điểm đóng góp); `null` nếu chi nhánh không có dữ liệu định lượng năm đó |
| `scoresByGroup2Qualitative` | object \| null | điểm theo **từng nhóm cấp 2 định tính**; `null` nếu không có dữ liệu định tính |

---

## 4. `compare_branches`

So sánh điểm rủi ro của 2 hoặc nhiều chi nhánh trong cùng 1 năm.

- **Endpoint**: `GET /api/audit/tools/compare-branches?branchCodes=A,B,C&year`
- **Permission**: `AUDIT.RISK_SCORING_EXEC.VIEW`
- **Nguồn**: gọi lặp `get_branch_risk` (không có dữ liệu mới ngoài tool 1)

**Input**: `branchCodes` (array of string, bắt buộc), `year` (integer, bắt buộc)

**Output**: `RiskBranchScoreCombinedRowResponse[]` — mã nào không có dữ liệu năm đó sẽ **bị bỏ khỏi mảng kết quả** (không trả `null` chen giữa).

---

## 5. `list_branches`

Tìm danh sách chi nhánh/đơn vị theo loại đơn vị và/hoặc từ khóa.

- **Endpoint**: `GET /api/audit/tools/branches?unitType&search&activeOnly`
- **Permission**: `AUDIT.RISK_SCORING_EXEC.VIEW`
- **Nguồn**: `AuditObjectUnitService.list()`

**Input** (tất cả tùy chọn)

| Field | Type | Ghi chú |
|---|---|---|
| `unitType` | string | mã từ danh mục `UNIT_TYPE` (vd `CN`, `HO`, `GSCC`) |
| `search` | string | khớp substring không phân biệt hoa/thường trên `code` hoặc `name` |
| `activeOnly` | boolean | `true` = chỉ lấy đơn vị đang hiệu lực |

**Output**: `AuditObjectUnitResponse[]` (cùng shape tool 2)

---

## 6. `get_risk_history`

Xem lịch sử điểm rủi ro của 1 chi nhánh qua nhiều năm.

- **Endpoint**: `GET /api/audit/tools/risk-history?branchCode&fromYear&toYear`
- **Permission**: `AUDIT.RISK_SCORING_EXEC.VIEW`
- **Nguồn**: gọi lặp `RiskBranchScoreCombinedService.list(year)` qua từng năm

**Input**: `branchCode` (bắt buộc); `fromYear`/`toYear` (tùy chọn — nếu bỏ trống, lấy toàn bộ khoảng năm có trong danh mục `YEAR`)

**Output**: `RiskBranchScoreCombinedRowResponse[]`, sắp xếp tăng dần theo năm — **chỉ chứa năm có dữ liệu thật**, năm không có điểm sẽ không xuất hiện trong mảng (không phải điểm 0).

---

## 7. `get_risk_criteria`

Xem định nghĩa chỉ tiêu chấm điểm (định lượng / định tính / chỉ tiêu ĐGRR khác).

- **Endpoint**: `GET /api/audit/tools/risk-criteria?kind=quantitative|qualitative|other`
- **Permission**: `AUDIT.RISK_SCORING_EXEC.VIEW`
- **Nguồn**: `CriteriaQuantitativeService.list()` / `CriteriaQualitativeService.list()` / `RiskCriteriaOtherService.list()`

**Input**: `kind` (bắt buộc, 1 trong 3 giá trị trên — giá trị khác trả lỗi `AUDIT_TOOLS_INVALID_KIND`)

**Output**: `RiskCriteriaToolResponse[]` — 1 shape chung cho cả 3 loại, field không áp dụng cho `kind` đang hỏi sẽ là `null`:

| Field | Type | Chỉ có ở `kind` |
|---|---|---|
| `kind` | string | tất cả |
| `code`, `name`, `weight`, `active` | string/number/boolean | tất cả |
| `group1Code`, `group2Code` | string \| null | quantitative, qualitative |
| `groupHoCode`, `riskTypeHoCode` | string \| null | other |
| `criteriaType`, `businessThreshold`, `viewThreshold`, `score20`..`score100`, `scoringGuide` | number/string \| null | quantitative |
| `impactLevel`, `likelihoodLevel` | integer \| null | qualitative |

---

## 8. `get_top_risk_branches`

Tìm N chi nhánh có điểm rủi ro cao nhất trong 1 năm.

- **Endpoint**: `GET /api/audit/tools/top-risk-branches?year&limit&unitType`
- **Permission**: `AUDIT.RISK_SCORING_EXEC.VIEW`
- **Nguồn**: `RiskBranchScoreCombinedService.list(year)`, sắp xếp giảm dần

**Input**: `year` (bắt buộc); `limit` (tùy chọn, mặc định 10); `unitType` (tùy chọn, lọc theo loại đơn vị)

**Output**: `RiskBranchScoreCombinedRowResponse[]`, tối đa `limit` phần tử, giảm dần theo `totalScore`.

---

## 9. `get_audit_findings`

Lấy các phát hiện kiểm toán đã ghi nhận cho 1 chi nhánh (hoặc toàn bộ, lọc theo khoảng ngày/mức độ).

- **Endpoint**: `GET /api/audit/tools/audit-findings?branchCode&fromDate&toDate&severity`
- **Permission**: `AUDIT.FINDING.VIEW`
- **Nguồn**: `AuditFindingService.search(...)` — bảng `audit_finding` (mới, xem mục "Ghi chú triển khai" bên dưới)

**Input** (tất cả tùy chọn): `branchCode` (string), `fromDate`/`toDate` (date `YYYY-MM-DD`), `severity` (string — mã từ danh mục `RISK_LEVEL`)

**Output**: `AuditFindingResponse[]`

| Field | Type |
|---|---|
| `id` | UUID |
| `branchCode`, `branchName` | string / string\|null |
| `title` | string |
| `description` | string \| null |
| `severity` | string (mã danh mục `RISK_LEVEL`) |
| `severityName` | string \| null (tên hiển thị) |
| `detectedDate` | date |
| `active` | boolean |

> **Mảng rỗng nghĩa là chưa ai nhập phát hiện nào** cho điều kiện lọc đó —
> đây là dữ liệu thật (chưa có), agent phải nói "chưa ghi nhận phát hiện
> nào", **không được tự suy ra** một phát hiện nào đó từ ngữ cảnh khác (kể
> cả field `keyFindings` tự do ở tool 2 — hai nguồn này KHÔNG đồng bộ với
> nhau).

---

## 10. `get_evidence`

Lấy danh sách file chứng minh (evidence) đính kèm cho 1 phát hiện kiểm toán.

- **Endpoint**: `GET /api/audit/tools/evidence?findingId`
- **Permission**: `AUDIT.FINDING.VIEW`
- **Nguồn**: `AttachmentService.listByEntity("AUDIT_FINDING", findingId)` (hạ tầng đính kèm dùng chung, `govia-core`)

**Input**: `findingId` (UUID, bắt buộc — phải là `id` trả về từ tool 9). Tool này **cố ý không nhận `entityName` tùy ý** để agent không dò được attachment của entity khác ngoài phạm vi Audit Finding.

**Output**: `EvidenceResponse[]`

| Field | Type |
|---|---|
| `id` | UUID |
| `fileName` | string |
| `contentType` | string \| null |
| `sizeBytes` | integer \| null |
| `downloadUrl` | string — endpoint thật `/api/attachments/{id}/download`, cần Bearer token khi tải |

> Mảng rỗng nghĩa là phát hiện đó chưa được đính kèm file nào — không suy diễn nội dung file.

---

## Bổ sung giai đoạn G1 (2026-10): 5 tool mới + phân tool theo agent

Từ G1, mỗi tool thuộc về **1 agent chuyên trách** (`AgentProfileRegistry`): model chỉ được thấy bộ tool
của agent mà A0 điều phối (`AgentRouter`) chọn cho câu hỏi, và `AgentOrchestratorService` chặn cứng
mọi tool ngoài bộ đó (ghi `agent_tool_call_log` với status `ERROR`).

| Agent | Tool |
|---|---|
| A0 Trợ lý chung | `get_my_tasks`, `search_screens`, `search_documents` |
| A1 Trợ lý Rủi ro | 10 tool ở trên + `get_score_changes`, `get_expert_rank_overrides` |

### 11. `get_score_changes`

Chi nhánh có điểm rủi ro tổng hợp biến động nhiều nhất giữa 2 năm.

- **Endpoint**: `GET /api/audit/tools/score-changes?year&compareYear&direction&limit`
- **Permission**: `AUDIT.RISK_SCORING_EXEC.VIEW`
- **Nguồn**: 2 lần `RiskBranchScoreCombinedService.list(...)` — không tính lại điểm
- **Input**: `year` (bắt buộc), `compareYear` (mặc định `year - 1`), `direction` (`increase` \| `decrease` \| bỏ trống = cả hai, xếp theo độ lớn), `limit` (mặc định 10)
- **Output**: `ScoreChangeResponse[]` — `branchCode`, `branchName`, `year`, `totalScore`, `rankLabel`, `previousYear`, `previousTotalScore`, `previousRankLabel`, `scoreChange` (= totalScore − previousTotalScore), `rankChanged`

> Chỉ chi nhánh có điểm ở **cả 2 năm** mới được so sánh.

### 12. `get_expert_rank_overrides`

Kết quả "Xếp hạng chuyên gia" đã lưu của 1 năm.

- **Endpoint**: `GET /api/audit/tools/expert-rank-overrides?year&onlyChanged`
- **Permission**: `AUDIT.RISK_SCORING_EXEC.VIEW`
- **Nguồn**: `RiskBranchScoreExpertRankService.list(year)` (chỉ đọc — không gọi `syncFromSource`)
- **Input**: `year` (bắt buộc), `onlyChanged` (mặc định `true` = chỉ dòng có `reRankLabel` khác `baseRankLabel`)
- **Output**: `RiskBranchScoreExpertRankResponse[]` như màn "Xếp hạng chuyên gia"

### 13. `get_my_tasks`

Việc đang chờ chính người dùng hiện tại xử lý trong quy trình phê duyệt (Flowable).

- **Endpoint**: `GET /api/workflow/tasks/my` (endpoint sẵn có của màn "Việc cần xử lý")
- **Permission**: `WORKFLOW.TASK.VIEW`
- **Input**: không có — luôn là người dùng đang hỏi
- **Output**: `TaskSummary[]`

### 14. `search_screens`

Tìm màn hình/chức năng trên menu theo từ khoá.

- **Không có endpoint riêng**: dữ liệu là danh sách menu mà frontend gửi kèm mỗi request chat
  (`AgentChatRequest.screens`, đã lọc theo quyền của chính người dùng trong `useAppMenu`), nên trợ lý
  không bao giờ chỉ tới màn hình người dùng không được vào.
- **Permission**: `AUDIT.AGENT.VIEW`
- **Input**: `query` (bắt buộc)
- **Output**: tối đa 8 phần tử `{label, group, path}`; giao diện chat biến `path` thành link bấm được.

### 15. `search_documents`

Tìm văn bản, quy định nội bộ trong Thư viện tài liệu (RAG giai đoạn G1).

- **Endpoint**: `GET /api/audit/agent/tools/documents?query&limit&includeExpired`
- **Permission**: `AUDIT.DOCUMENT_LIBRARY.VIEW`
- **Nguồn**: `AuditDocumentLibraryService.list()` — chỉ mục giữ **trong bộ nhớ** theo tenant, cập nhật
  tăng dần mỗi lần tìm (văn bản mới/sửa mới embed lại); không thêm cột/bảng vào dữ liệu nghiệp vụ.
- **Cách xếp hạng**: 0.7 × cosine (khi cấu hình `govia.llm.embedding.model`, vd `bge-m3`) + 0.3 × điểm từ
  khoá (âm tiết + cặp âm tiết, trọng số IDF, không dấu). Không cấu hình embedding thì chỉ dùng từ khoá.
- **Input**: `query` (bắt buộc), `limit` (mặc định 5, tối đa 10), `includeExpired` (mặc định `false`)
- **Output**: `KnowledgeHit[]` — `documentId`, `documentNumber`, `documentName`, `topic`, `businessActivity`,
  `issueDate`, `effectiveDate`, `expired`, `expiryDate`, `legalBasis`, `excerpt` (≤ 600 ký tự), `score`,
  `matchMode` (`semantic` \| `keyword`)

---

## Ghi chú triển khai: module `AuditFinding` (mới)

Trước đợt chuẩn hóa này, hệ thống **không có bảng "phát hiện kiểm toán" có
cấu trúc** — chỉ có field tự do `AuditObjectUnit.keyFindings`. Để tool 9 và
10 có dữ liệu thật ngay từ đầu, đã thêm:

- Bảng `audit_finding` (migration `045-audit-finding-schema.yaml`)
- Permission `AUDIT.FINDING.VIEW/CREATE/EDIT/DELETE` (migration `046-...`)
- Seed danh mục `RISK_LEVEL` (Cao/Trung bình/Thấp) — danh mục này đã khai
  báo sẵn trong `AuditMasterDataCategory` nhưng chưa từng được dùng, nay
  dùng làm giá trị cho `AuditFinding.severity` (migration `047-...`)
- Màn hình CRUD `AuditFindingPage`/`AuditFindingTable` (menu "Chấm Điểm" →
  "Phát hiện kiểm toán") — **là nơi duy nhất** để nhập dữ liệu cho 2 tool
  này; nếu chưa ai dùng màn hình đó, `get_audit_findings`/`get_evidence` sẽ
  luôn trả mảng rỗng (đúng, không phải lỗi).

## Nguyên tắc bắt buộc khi nối AI Agent vào các tool này

1. Agent chỉ được gọi qua `/api/audit/tools/*` — không tự query DB, không
   tự gọi các endpoint CRUD khác (những endpoint đó có thể ghi dữ liệu).
2. Agent xác thực như 1 user bình thường (Bearer JWT) — không có quyền
   "bypass" nào riêng cho agent; nếu user hỏi vượt quá quyền của họ, endpoint
   trả 403 như mọi API khác.
3. Khi 1 tool trả `null`/mảng rỗng, agent phải trình bày đúng là "chưa có dữ
   liệu" — không được suy diễn, ước lượng, hay bịa số liệu thay thế.
4. Schema ở tài liệu này phải khớp 100% với code thật (`AuditToolsController`
   + `AuditToolsService`). Khi sửa 1 trong 2 file đó, cập nhật tài liệu này
   trong cùng 1 lần đổi.

## Bổ sung giai đoạn G2 (2026-10): A3, A4, A7 + soạn nháp

| Agent | Tool |
|---|---|
| A4 Trợ lý Phát hiện & Kiến nghị | `list_my_engagements`, `get_ttss_summary`, `get_ttss_records`, `list_engagement_recommendations`, `search_similar_findings` |
| A3 Trợ lý Tác nghiệp KT | `list_my_engagements`, `get_engagement_work_items`, `search_catalog` |
| A7 Trợ lý Danh mục | `search_catalog`, `find_catalog_duplicates` |

Tất cả nằm trong `AgentWorkToolsService` và **chỉ gọi lại method của controller nghiệp vụ sẵn có** (không
gọi repository), nên giữ nguyên `@PreAuthorize` và phân quyền theo dòng (vd TTSS: thành viên chỉ thấy dòng
của mình, trưởng nhóm thấy nhóm mình, trưởng đoàn/VIEW_ALL thấy tất cả). Tham số `engagement` nhận **mã hoặc
id** cuộc kiểm toán, nhưng chỉ trong danh sách `GET /api/audit/plan/engagement/assigned` của người hỏi.

| Tool | Nguồn (endpoint sẵn có) | Quyền | Ghi chú |
|---|---|---|---|
| `list_my_engagements(search?, limit?)` | `/api/audit/plan/engagement/assigned` | `AUDIT.PLAN_ENGAGEMENT.VIEW` | tối đa 30 |
| `get_engagement_work_items(engagement, phase?, status?)` | `/api/audit/plan/engagement/{id}/work-management` × CBKT/THKT/DCKT | `AUDIT.WORK_MANAGEMENT.VIEW` | kèm đếm theo trạng thái/giai đoạn, tối đa 60 dòng |
| `get_ttss_summary(engagement)` | `/api/audit/plan/engagement/{id}/ttss` | `AUDIT.TTSS.VIEW` | tổng, trọng yếu, chưa gắn KN, tổng tiền, top 15 phát hiện |
| `get_ttss_records(engagement, businessSegmentCode?, findingCode?, materialOnly?, withoutRecommendationOnly?, limit?)` | như trên | `AUDIT.TTSS.VIEW` | rút gọn, tối đa 50 |
| `list_engagement_recommendations(engagement)` | `/api/audit/plan/engagement/{id}/ttss/recommendations` | `AUDIT.TTSS.VIEW` | |
| `search_similar_findings(query, excludeEngagement?, limit?)` | 2 endpoint trên, lặp các CKT được phân công (≤ 30) | `AUDIT.TTSS.VIEW` | so khớp từ khoá ≥ 50% |
| `search_catalog(catalog, query?, businessSegmentCode?, limit?)` | `list()` của 10 danh mục: `control_point(_qt)`, `work_item(_qt)`, `exception_type(_qt)`, `exception_mapping(_qt)`, `process_step(_qt)` | quyền VIEW của danh mục tương ứng | bỏ các cột id nội bộ |
| `find_catalog_duplicates(catalog, limit?)` | như trên | như trên | nhóm theo tên (bỏ dấu, không phân biệt hoa thường) + nghiệp vụ/năm/bộ công việc/giai đoạn |

### Soạn nháp (mức M2 — không có tool ghi)

| Endpoint | Quyền | Đầu vào | Kiểm soát |
|---|---|---|---|
| `POST /api/audit/agent/drafts/recommendations` | `AUDIT.AGENT.VIEW` + `AUDIT.TTSS.VIEW` | `engagementId`, `businessSegmentId?`, `instruction?` | Hệ thống tự lấy TTSS (ưu tiên dòng chưa gắn KN), nhóm theo mã phát hiện (≤ 12 nhóm), kèm KN đã có và KN tương tự ở CKT khác; 1 lần gọi model qua tool `submit_recommendation_drafts`. Bản nháp có mã phát hiện không có trong dữ liệu hoặc số (≥ 3 chữ số) không có trong dữ liệu ⇒ `grounded=false`. **Không lưu gì** — người dùng bấm "Dùng nội dung này" rồi tự bấm Thêm. |
| `POST /api/audit/agent/drafts/rewrite` | `AUDIT.AGENT.VIEW` | `text`, `purpose` (`RECOMMENDATION`/`FINDING`/`SUMMARY`) | Viết lại giữ nguyên sự kiện; số không có trong văn bản gốc ⇒ `grounded=false`. Người dùng bấm "Thay thế" mới đổi nội dung ô nhập. |

Cả hai tôn trọng công tắc `govia.agent.enabled` và `govia.agent.disabled-agents=A4` (trả 503), mô hình lỗi
trả 503 `AGENT_LLM_UNAVAILABLE`, và ghi `agent_tool_call_log` (`draft_recommendations` / `rewrite_text`).
