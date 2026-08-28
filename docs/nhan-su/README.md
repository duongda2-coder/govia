# Module Nhân sự

Tài liệu mô tả logic nghiệp vụ của module Nhân sự trong GOVIA — Nhân viên (Employee), Danh mục Chức vụ, Đơn vị tổ chức (OrganizationUnit). Toàn bộ nội dung được rút trực tiếp từ code hiện có tại `backend/govia-identity/src/main/java/com/govia/identity/{service,controller,entity}` (không suy diễn).

> **Lưu ý:** bảng `position` riêng (module Chức danh cũ) đã bị **gỡ bỏ hoàn toàn**. "Chức vụ" của Employee giờ tham chiếu tới `AuditMasterDataItem` (category `POSITION`, bảng dùng chung `audit_master_data_item` với các danh mục khác của module Kiểm toán nội bộ), quản lý qua `PositionCatalogController` (`/api/people/positions`, quyền `PEOPLE.POSITION.*` riêng — không dùng chung `AUDIT.MASTER_DATA.*`). Dữ liệu cũ trong bảng `position` đã được migrate sang `audit_master_data_item` giữ nguyên UUID (xem changelog `033-migrate-position-to-master-data.yaml`).

## 1. Mô hình tổng quan

3 thực thể liên kết với nhau như sau:

```
OrganizationUnit (cây 4 cấp, tự tham chiếu qua parentId)
    ▲  quản lý bởi (managerEmployeeId)
    │
Employee ──▶ orgUnitId (đơn vị đang thuộc về)
    │    ──▶ positionId (chức vụ — trỏ tới AuditMasterDataItem, category=POSITION)
    │    ──▶ managerId (quản lý trực tiếp — tự tham chiếu tới Employee khác)
    ▼
UserAccount (0 hoặc 1 tài khoản đăng nhập — xem tài liệu module Quản trị hệ thống)

AuditMasterDataItem (category=POSITION) — "Danh mục Chức vụ", master-data phẳng, không phân cấp,
    dùng chung bảng audit_master_data_item với các danh mục khác của module Kiểm toán nội bộ
```

`Employee` là **"danh tính gốc"** mà mọi module khác trong platform tham chiếu tới qua `employeeCode` (theo comment trong `Employee.java`).

## 2. Nhân viên (Employee)

**Controller:** `EmployeeController` (`/api/employees`) — mỗi thao tác gate bởi 1 quyền riêng trong catalog `PEOPLE.EMPLOYEE.*` (không dùng `hasRole` cứng như module Quản trị).

| Endpoint | Method | Quyền yêu cầu | Mô tả |
|---|---|---|---|
| `/api/employees` | GET | `VIEW` | Danh sách có phân trang + lọc (`EmployeeFilter`) |
| `/api/employees/{id}` | GET | `VIEW` | Chi tiết 1 nhân viên |
| `/api/employees` | POST | `CREATE` | Tạo mới |
| `/api/employees/{id}` | PUT | `EDIT` | Cập nhật |
| `/api/employees/{id}/status` | PATCH | `EDIT` | Đổi trạng thái |
| `/api/employees/{id}` | DELETE | `DELETE` | Xoá cứng |
| `/api/employees/{id}/account` | POST | `EDIT` | Tạo tài khoản đăng nhập (xem module Quản trị hệ thống) |
| `/api/employees/{id}/account/reset-password` | PATCH | `SUPER_ADMIN` role | Admin đặt lại mật khẩu hộ |
| `/api/employees/import` | POST | `IMPORT` | Nhập hàng loạt từ Excel |
| `/api/employees/export/excel` | GET | `EXPORT` | Xuất Excel (áp dụng cùng bộ lọc đang xem) |
| `/api/employees/export/word` | GET | `EXPORT` | Xuất Word |

### 2.1. Trạng thái (`EmployeeStatus`)

```
ACTIVE | ON_LEAVE | TERMINATED
```

- Nhân viên **mới tạo luôn `ACTIVE`** — không có bước duyệt hay trạng thái chờ nào ở luồng tạo mặc định.
- Đổi trạng thái qua `PATCH /{id}/status` là **tự do giữa 3 trạng thái**, không có ma trận chuyển trạng thái bị chặn — service không kiểm tra trạng thái hiện tại trước khi cho đổi (vd đổi thẳng từ `TERMINATED` về `ACTIVE` vẫn được).

> Trước đây có thêm `PENDING_APPROVAL`/`REJECTED` gắn với 1 workflow duyệt tạo nhân viên tự động — module workflow đó đã bị gỡ bỏ hoàn toàn khỏi hệ thống (xem ghi chú cuối tài liệu), nên hiện tại chỉ còn đúng 3 trạng thái trên và việc tạo nhân viên không còn cổng duyệt nào.

### 2.2. Ràng buộc khi tạo/sửa (`create`, `update`)

Thứ tự kiểm tra, dừng ở lỗi đầu tiên:

1. `employeeCode` duy nhất **trong tenant** (`EMPLOYEE_CODE_DUPLICATE`) — khi sửa, chỉ kiểm tra nếu mã thực sự đổi.
2. `orgUnitId` (nếu có) phải tồn tại và cùng tenant (`ORG_UNIT_NOT_FOUND`).
3. `positionId` (nếu có) phải tồn tại, cùng tenant, và đúng category `POSITION` trong `audit_master_data_item` (`POSITION_NOT_FOUND`).
4. `managerId` (nếu có):
   - Phải tồn tại và cùng tenant (`EMPLOYEE_MANAGER_NOT_FOUND`).
   - **Chỉ khi sửa** (không áp dụng lúc tạo mới, vì nhân viên mới chưa có ai là cấp dưới): không được tự chọn chính mình làm quản lý (`EMPLOYEE_INVALID_MANAGER`), và không được tạo **vòng lặp báo cáo** — đi ngược chuỗi quản lý của người được chọn, nếu gặp lại chính nhân viên đang sửa thì chặn (`EMPLOYEE_MANAGER_CIRCULAR`). Ví dụ: A đang quản lý B, không thể sửa A để B trở thành quản lý của A.

### 2.3. Xoá (xoá cứng, có ràng buộc tham chiếu)

`delete()` xoá cứng khỏi DB, nhưng **chặn nếu còn bất kỳ tham chiếu nào tới nhân viên đó**, theo đúng thứ tự kiểm tra:

1. Đang là quản lý trực tiếp của người khác → `EMPLOYEE_HAS_SUBORDINATES`.
2. Đang gắn với 1 tài khoản đăng nhập → `EMPLOYEE_HAS_USER_ACCOUNT`.
3. Đang là trưởng của 1 đơn vị tổ chức (`OrganizationUnit.managerEmployeeId`) → `EMPLOYEE_IS_ORG_UNIT_MANAGER`.

Phải gỡ hết 3 ràng buộc trên (đổi quản lý cho cấp dưới, xoá tài khoản, đổi trưởng đơn vị khác) trước khi xoá được — tránh dữ liệu mồ côi / lỗi khoá ngoại.

### 2.4. Lọc danh sách (`EmployeeFilter`)

Hỗ trợ lọc kết hợp (AND) theo: `orgUnitId`, `status`, từ khoá tự do (`keyword`), và lọc theo từng trường cụ thể (`employeeCode`, `fullName`, `phone`, `email`, tên đơn vị, tên chức danh, tên quản lý) — dùng Spring Data `Specification`, áp dụng đồng thời cho cả xem danh sách lẫn xuất Excel/Word (xuất luôn tôn trọng bộ lọc đang xem trên UI).

### 2.5. Nhập Excel (`importFromExcel`)

Đọc đúng mẫu đã xuất (`exportColumns`). Với mỗi dòng:
- Bắt buộc có `employeeCode` và `fullName`, thiếu 1 trong 2 → lỗi dòng đó (`IMPORT_MISSING_REQUIRED`), **không làm hỏng cả file** — các dòng hợp lệ khác vẫn được tạo.
- Đơn vị/chức vụ tham chiếu theo **tên** (không phân biệt hoa/thường), không tìm thấy → lỗi dòng (`IMPORT_ORG_UNIT_NOT_FOUND` / `IMPORT_POSITION_NOT_FOUND`).
- Cột trạng thái nhận đúng tên enum (`ACTIVE`/`ON_LEAVE`/`TERMINATED`), để trống mặc định `ACTIVE`, giá trị lạ → `IMPORT_INVALID_STATUS`.
- Mỗi dòng tạo qua đúng `create()` (đi qua đầy đủ validate ở mục 2.2), rồi đổi trạng thái riêng nếu khác `ACTIVE` — nghĩa là nhân viên nhập từ Excel **luôn được tạo `ACTIVE` trước**, sau đó mới chuyển trạng thái theo cột trong file (không tạo thẳng ở trạng thái khác).
- Kết quả trả về đếm số dòng thành công + danh sách lỗi theo từng số dòng cụ thể.

## 3. Đơn vị tổ chức (OrganizationUnit)

**Controller:** `OrganizationUnitController` (`/api/org-units`), quyền catalog `PEOPLE.ORGUNIT.*`.

| Endpoint | Method | Quyền | Mô tả |
|---|---|---|---|
| `/api/org-units` | GET | `VIEW` | Danh sách phẳng |
| `/api/org-units/tree` | GET | `VIEW` | Dạng cây (dựng từ danh sách phẳng theo `parentId`) |
| `/api/org-units/{id}` | GET | `VIEW` | Chi tiết |
| `/api/org-units` | POST | `CREATE` | Tạo mới |
| `/api/org-units/{id}` | PUT | `EDIT` | Cập nhật |
| `/api/org-units/{id}/active` | PATCH | `EDIT` | Bật/tắt hoạt động |
| `/api/org-units/{id}` | DELETE | `DELETE` | Xoá cứng |
| `/api/org-units/import` | POST | `IMPORT` | Nhập hàng loạt từ Excel |
| `/api/org-units/export/excel`, `/export/word` | GET | `EXPORT` | Xuất danh sách |

### 3.1. Cây tổ chức 4 cấp (`levelCode`)

Quy ước cố định trong code (`OrganizationUnitService.ALLOWED_LEVEL_CODES`):

| `levelCode` | Ý nghĩa |
|---|---|
| `001` | Khối |
| `002` | Trung tâm |
| `003` | Phòng ban |
| `004` | Bộ phận |

`levelCode` là **tuỳ chọn** (không bắt buộc phải có) nhưng nếu có giá trị thì bắt buộc phải nằm trong 4 mã trên, sai → `ORG_UNIT_INVALID_LEVEL_CODE`. Cấu trúc cây **không ép buộc** đơn vị cấp dưới phải đúng 1 cấp thấp hơn cấp cha (vd không có kiểm tra "Bộ phận (004) phải có cha là Phòng ban (003)") — `levelCode` hiện chỉ mang tính phân loại/hiển thị, quan hệ cây thực tế hoàn toàn do `parentId` quyết định.

### 3.2. Ràng buộc khi tạo/sửa

1. `code` duy nhất trong tenant (`ORG_UNIT_CODE_DUPLICATE`).
2. `levelCode` hợp lệ nếu có (mục 3.1).
3. `parentId` (nếu có) phải tồn tại, cùng tenant (`ORG_UNIT_PARENT_NOT_FOUND`).
4. **Chỉ khi sửa:** không được chọn cha tạo thành **vòng lặp** — đi ngược chuỗi cha của `parentId` mới, nếu gặp lại chính đơn vị đang sửa thì chặn (`ORG_UNIT_CIRCULAR`, đồng thời có bảo vệ chống lặp vô hạn nếu dữ liệu cha-con bị hỏng sẵn).
5. `managerEmployeeId` (nếu có) phải tồn tại, cùng tenant (`ORG_UNIT_MANAGER_NOT_FOUND`).

### 3.3. Bật/tắt hoạt động (`setActive`)

Đơn giản chỉ đổi cờ `active` — **không cascade** xuống đơn vị con hay nhân viên thuộc đơn vị (tắt 1 đơn vị cha không tự động tắt các đơn vị con). Audit log ghi nhận hành động là `DELETE` khi tắt, `UPDATE` khi bật (không phải xoá thật, chỉ mượn action code để phân biệt trên nhật ký).

### 3.4. Xoá (xoá cứng, có ràng buộc tham chiếu)

`delete()` xoá cứng khỏi DB, chặn nếu:

1. Đang là đơn vị cha của 1 đơn vị khác (`parentId` của đơn vị khác trỏ tới nó) → `ORG_UNIT_HAS_CHILDREN`.
2. Đang có nhân viên trực thuộc (`Employee.orgUnitId` trỏ tới nó) → `ORG_UNIT_HAS_EMPLOYEES`.

Không kiểm tra tham chiếu từ `ApprovalMatrixRule.orgUnitId` (ma trận phê duyệt) — nếu đơn vị bị xoá đang gắn 1 quy tắc phê duyệt riêng, quy tắc đó sẽ mồ côi (không lỗi, chỉ không còn khớp đơn vị nào khi tra cứu).

### 3.5. Nhập Excel (`importFromExcel`)

Khác với Employee, đơn vị cha và trưởng đơn vị tham chiếu theo **mã** (`parentCode`, `managerEmployeeCode`), không phải theo tên — dễ đối chiếu/sửa tay trên Excel. **Lưu ý quan trọng ghi rõ trong code:** đơn vị cha phải nằm ở dòng **trước** đơn vị con trong cùng file (import xử lý tuần tự từng dòng, chưa hỗ trợ sắp xếp lại theo quan hệ cha-con hay 2-pass). Không tìm thấy mã cha/mã quản lý tương ứng → lỗi riêng từng dòng (`IMPORT_PARENT_NOT_FOUND`, `IMPORT_MANAGER_NOT_FOUND`), không hỏng cả file.

## 4. Danh mục Chức vụ

**Controller:** `PositionCatalogController` (`/api/people/positions`), quyền catalog `PEOPLE.POSITION.*`. Dữ liệu là 1 category (`POSITION`) trong bảng `audit_master_data_item` dùng chung với các danh mục khác của module Kiểm toán nội bộ (`MasterDataItemService`/`AuditMasterDataItemRepository`) — controller chỉ cố định `category=POSITION` và gate bằng permission riêng `PEOPLE.POSITION.*` (khác `AUDIT.MASTER_DATA.*` mà các danh mục Kiểm toán khác dùng), vì đây là danh mục thuộc module Nhân sự.

| Endpoint | Method | Quyền | Mô tả |
|---|---|---|---|
| `/api/people/positions` | GET | `VIEW` | Danh sách |
| `/api/people/positions` | POST | `CREATE` | Tạo mới |
| `/api/people/positions/{id}` | PUT | `EDIT` | Cập nhật |
| `/api/people/positions/{id}` | DELETE | `DELETE` | Xoá cứng |
| `/api/people/positions/import` | POST | `IMPORT` | Nhập từ Excel |
| `/api/people/positions/export/excel`, `/export/word` | GET | `EXPORT` | Xuất danh sách |

So với "Chức danh" (bảng `position`) cũ: có thêm các trường `description`, `validFrom`, `validTo`, `sortOrder` (giống các danh mục Kiểm toán khác); xoá là **xoá cứng** (không còn kiểu bật/tắt qua `PATCH /active` như trước) và **không kiểm tra** còn Employee/AuditDocumentLibrary nào đang tham chiếu tới bản ghi bị xoá hay không (xoá xong các bản ghi liên quan sẽ có `positionId`/`issuerPositionId` trỏ tới ID không còn tồn tại — tên hiển thị trả về `null`, không lỗi). Ràng buộc duy nhất khi tạo/sửa: `code` duy nhất **trong cùng category `POSITION`** của tenant (`MASTER_DATA_CODE_DUPLICATE`).

## 5. Enum liên quan

| Enum | Giá trị | Dùng ở |
|---|---|---|
| `EmployeeStatus` | `ACTIVE`, `ON_LEAVE`, `TERMINATED` | Employee.status |
| `Gender` | `MALE`, `FEMALE`, `OTHER` | Employee.gender (không bắt buộc) |

## 6. Bảng tổng hợp mã lỗi nghiệp vụ

| Mã lỗi | HTTP | Khi nào |
|---|---|---|
| `EMPLOYEE_CODE_DUPLICATE` | 400 | Trùng mã nhân viên trong tenant |
| `ORG_UNIT_NOT_FOUND` | 400 | `orgUnitId` không tồn tại/khác tenant khi tạo/sửa Employee |
| `POSITION_NOT_FOUND` | 400 | `positionId` không tồn tại/khác tenant khi tạo/sửa Employee |
| `EMPLOYEE_MANAGER_NOT_FOUND` | 400 | `managerId` không tồn tại/khác tenant |
| `EMPLOYEE_INVALID_MANAGER` | 400 | Tự chọn chính mình làm quản lý |
| `EMPLOYEE_MANAGER_CIRCULAR` | 400 | Chọn quản lý tạo thành vòng lặp báo cáo |
| `EMPLOYEE_NOT_FOUND` | 404 | Nhân viên không tồn tại/khác tenant |
| `EMPLOYEE_HAS_SUBORDINATES` | 400 | Xoá nhân viên đang là quản lý của người khác |
| `EMPLOYEE_HAS_USER_ACCOUNT` | 400 | Xoá nhân viên đang có tài khoản đăng nhập |
| `EMPLOYEE_IS_ORG_UNIT_MANAGER` | 400 | Xoá nhân viên đang là trưởng 1 đơn vị |
| `IMPORT_MISSING_REQUIRED` | — (lỗi theo dòng) | Thiếu trường bắt buộc khi nhập Excel |
| `IMPORT_ORG_UNIT_NOT_FOUND` / `IMPORT_POSITION_NOT_FOUND` | — | Tên đơn vị/chức danh trong Excel không khớp dữ liệu có sẵn |
| `IMPORT_INVALID_STATUS` | — | Cột trạng thái trong Excel không đúng enum |
| `ORG_UNIT_CODE_DUPLICATE` | 400 | Trùng mã đơn vị trong tenant |
| `ORG_UNIT_INVALID_LEVEL_CODE` | 400 | `levelCode` ngoài 4 giá trị cho phép |
| `ORG_UNIT_PARENT_NOT_FOUND` | 400 | Đơn vị cha không tồn tại/khác tenant |
| `ORG_UNIT_CIRCULAR` | 400 | Chọn cha tạo thành vòng lặp cây tổ chức |
| `ORG_UNIT_MANAGER_NOT_FOUND` | 400 | Trưởng đơn vị chỉ định không tồn tại/khác tenant |
| `ORG_UNIT_NOT_FOUND` | 404 | Đơn vị không tồn tại/khác tenant (khi thao tác trực tiếp trên đơn vị) |
| `ORG_UNIT_HAS_CHILDREN` | 400 | Xoá đơn vị đang là cha của 1 đơn vị khác |
| `ORG_UNIT_HAS_EMPLOYEES` | 400 | Xoá đơn vị đang có nhân viên trực thuộc |
| `IMPORT_PARENT_NOT_FOUND` / `IMPORT_MANAGER_NOT_FOUND` | — | Mã cha/mã quản lý trong Excel không khớp dữ liệu có sẵn |
| `MASTER_DATA_CODE_DUPLICATE` | 400 | Trùng mã trong cùng category (kể cả `POSITION`) của tenant |
| `MASTER_DATA_ITEM_NOT_FOUND` | 404 | Bản ghi danh mục không tồn tại/khác tenant/khác category (khi sửa/xoá trực tiếp qua `PositionCatalogController` hoặc `MasterDataItemController`) |

## 7. Ghi chú: workflow duyệt đã bị gỡ bỏ

Tại thời điểm viết tài liệu này, `EmployeeService.create()` **không** kích hoạt bất kỳ workflow phê duyệt nào — nhân viên mới luôn vào thẳng `ACTIVE`. Trước đó hệ thống từng có 1 module Workflow Engine (định nghĩa quy trình, duyệt nhiều cấp qua `WorkflowTriggerRule`) gắn vào đúng điểm này để tự động yêu cầu duyệt nhân viên mới theo đơn vị, nhưng đã được **gỡ bỏ hoàn toàn** (code, schema DB, migration, màn hình) theo yêu cầu chủ dự án vì không đáp ứng nhu cầu — có bản sao lưu code/schema riêng ngoài phạm vi tài liệu này nếu cần khôi phục sau này.
