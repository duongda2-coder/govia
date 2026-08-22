# Module Quản trị hệ thống

Tài liệu mô tả logic nghiệp vụ của module Quản trị hệ thống trong GOVIA — xác thực, vai trò (Role), quyền (Permission), và tài khoản đăng nhập (UserAccount). Toàn bộ nội dung được rút trực tiếp từ code hiện có tại `backend/govia-identity/src/main/java/com/govia/identity/{service,controller,entity}` (không suy diễn).

## 1. Mô hình tổng quan

GOVIA là hệ thống **multi-tenant**: mọi bảng dữ liệu đều có cột `tenant_id`, và mọi truy vấn đều lọc theo tenant của người dùng đang đăng nhập (`TenantContext.getTenantId()`, được set khi xác thực JWT). Không có cách nào 1 tenant nhìn thấy hoặc thao tác dữ liệu của tenant khác qua các API mô tả dưới đây.

Phân quyền dùng mô hình **RBAC 2 lớp**:

```
UserAccount (tài khoản đăng nhập)
    │  (n-n qua bảng user_role)
    ▼
Role (vai trò, vd "SUPER_ADMIN", "HR_MANAGER")
    │  (n-n qua bảng role_permission)
    ▼
Permission (quyền nguyên tử, vd "PEOPLE.EMPLOYEE.VIEW")
```

- 1 tài khoản có thể được gán **nhiều vai trò**; quyền hiệu lực của tài khoản = **hợp (union)** quyền của tất cả vai trò đang gán.
- Tài khoản **mới tạo mặc định không có vai trò nào** → không làm được gì cho đến khi admin gán vai trò (`AccountController.assignRoles`).
- 1 vai trò tồn tại **độc lập với Employee** — vai trò gắn với `UserAccount`, không gắn trực tiếp với nhân viên.

## 2. Xác thực (Auth)

**Controller:** `AuthController` (`/api/auth`) — là **điểm xác thực duy nhất** của toàn platform; mọi module khác chỉ *verify* JWT do đây phát hành (qua `JwtAuthenticationFilter` ở `govia-core`), không tự làm login riêng.

| Endpoint | Method | Public? | Mô tả |
|---|---|---|---|
| `/api/auth/login` | POST | Có | Đăng nhập bằng `tenantCode` + `username` + `password` |
| `/api/auth/refresh` | POST | Có | Cấp access token mới từ refresh token |
| `/api/auth/password` | PATCH | Không (cần đăng nhập) | Tự đổi mật khẩu của chính mình |

### 2.1. Đăng nhập (`login`)

Thứ tự kiểm tra trong `AuthService.login()` — dừng ở bước đầu tiên thất bại:

1. `tenantCode` phải tồn tại (`TENANT_NOT_FOUND`, 401).
2. Tenant phải ở trạng thái `ACTIVE` — nếu `SUSPENDED` thì từ chối (`TENANT_SUSPENDED`, 401) dù mật khẩu đúng.
3. `username` phải tồn tại **trong đúng tenant đó** (`INVALID_CREDENTIALS`, 401) — cùng username ở tenant khác không tính.
4. Tài khoản phải ở trạng thái `ACTIVE` (`USER_NOT_ACTIVE`, 401) — `LOCKED`/`DISABLED` bị chặn dù mật khẩu đúng.
5. Mật khẩu phải khớp hash đã lưu (BCrypt qua `PasswordEncoder`) — sai thì cùng lỗi `INVALID_CREDENTIALS` như bước 3 (**không tiết lộ** username có tồn tại hay không).

Đăng nhập thành công: cập nhật `lastLoginAt`, phát hành cặp **access token + refresh token**.

### 2.2. Nội dung JWT access token

Access token nhúng sẵn (không cần gọi thêm API nào để biết quyền của user):

- `userId`, `username`, `tenantId`
- `employeeCode` (null nếu tài khoản không gắn Employee — tài khoản hệ thống/tích hợp)
- `roles`: danh sách **mã** vai trò đang gán
- `permissions`: danh sách **mã** quyền hiệu lực (đã tính hợp từ mọi vai trò)

**Quy tắc quyền wildcard:** nếu bất kỳ vai trò nào của user có quyền mã `"*"` (chỉ được seed sẵn cho `SUPER_ADMIN`, xem mục 6) thì `permissions` trả về **TOÀN BỘ** mã quyền hiện có trong catalog (trừ chính `"*"`) — đảm bảo SUPER_ADMIN luôn full quyền kể cả khi sau này có thêm màn hình/quyền mới mà không cần cấu hình lại. Tài khoản chưa được gán vai trò nào nhận `permissions = []`.

**Thời hạn token** (`application.yml`, ghi đè được qua biến môi trường):
- Access token: 30 phút (`govia.security.jwt.access-token-minutes`)
- Refresh token: 7 ngày (`govia.security.jwt.refresh-token-days`)

### 2.3. Đổi mật khẩu (tự phục vụ)

`PATCH /api/auth/password` — yêu cầu đã đăng nhập, luôn thao tác trên chính tài khoản của người gọi (lấy từ `CurrentUserPrincipal`, không nhận `userId` từ client). Bắt buộc cung cấp đúng mật khẩu hiện tại (`INVALID_CURRENT_PASSWORD` nếu sai) trước khi đổi. Có ghi Audit Log.

## 3. Vai trò & Phân quyền (Role)

**Controller:** `RoleController` (`/api/roles`), `PermissionController` (`/api/permissions`) — **toàn bộ 2 controller này chỉ SUPER_ADMIN gọi được** (`@PreAuthorize("hasRole('SUPER_ADMIN')")` ở cấp class, không dùng permission catalog thông thường) vì đây là cấu hình phân quyền cho cả hệ thống, không thể/không nên delegate cho vai trò khác tự quản lý.

| Endpoint | Method | Mô tả |
|---|---|---|
| `/api/roles` | GET | Danh sách vai trò trong tenant |
| `/api/roles` | POST | Tạo vai trò mới |
| `/api/roles/{id}` | PUT | Sửa vai trò |
| `/api/roles/{id}` | DELETE | Xoá vai trò |
| `/api/roles/{id}/permissions` | GET | Danh sách mã quyền đang gán cho vai trò |
| `/api/roles/{id}/permissions` | PUT | **Ghi đè toàn bộ** quyền của vai trò |
| `/api/roles/export/excel` | GET | Xuất danh sách vai trò ra Excel |
| `/api/roles/{id}/permissions/export` | GET | Xuất ma trận phân quyền của 1 vai trò |
| `/api/roles/{id}/permissions/import` | POST | Nhập lại ma trận phân quyền từ Excel |
| `/api/permissions` | GET | Danh mục toàn bộ quyền trong platform (để vẽ UI) |

### 3.1. Vai trò hệ thống (`systemDefined`)

Vai trò có cờ `systemDefined = true` (hiện chỉ có `SUPER_ADMIN`, được seed sẵn — xem mục 6) **không thể sửa, không thể xoá, không thể đổi quyền** qua UI (`ROLE_SYSTEM_DEFINED`) — chặn ở cả 3 thao tác `update()`, `delete()`, `setPermissionCodes()`. Vai trò do admin tự tạo (`systemDefined = false`) không bị giới hạn này.

### 3.2. Ràng buộc khi tạo/sửa/xoá vai trò

- **Mã vai trò (`code`) duy nhất trong tenant** — trùng thì `ROLE_CODE_DUPLICATE`. Khi sửa, chỉ kiểm tra trùng nếu `code` thực sự đổi.
- **Không xoá được vai trò đang được gán cho bất kỳ tài khoản nào** (`ROLE_IN_USE`) — phải gỡ khỏi mọi tài khoản trước.
- Xoá vai trò tự động dọn sạch các dòng `role_permission` liên quan.

### 3.3. Gán quyền cho vai trò (`setPermissionCodes`)

Hành vi là **ghi đè hoàn toàn** (xoá hết `role_permission` cũ của vai trò rồi tạo lại theo danh sách mã quyền gửi lên), không phải cộng dồn. Mã quyền không khớp danh mục (`Permission`) bị bỏ qua âm thầm (không lỗi).

### 3.4. Xuất/Nhập ma trận phân quyền qua Excel

- **Xuất** (`exportPermissionsExcel`): dạng **danh sách phẳng** — mỗi dòng là 1 quyền (cột Module, Màn hình, Mã quyền, Mô tả, cột "Được cấp" đánh dấu `X` nếu vai trò đang có quyền đó). Chọn dạng phẳng thay vì ma trận cột-theo-action để scale được với hàng trăm màn hình mà không cần thêm cột mỗi khi có hành động mới.
- **Nhập** (`importPermissionsExcel`): đọc đúng file đã xuất, dòng nào có đánh dấu ở cột "Được cấp" (bất kỳ ký tự nào, không chỉ `X`) thì quyền đó được gán. Mã quyền không hợp lệ bị báo lỗi theo từng dòng (không làm hỏng cả file). Kết quả nhập **ghi đè toàn bộ** quyền hiện tại của vai trò (giống hành vi Lưu trên UI), không phải cộng dồn.

## 4. Tài khoản người dùng (UserAccount)

**Controller:** `AccountController` (`/api/accounts`) — cũng chỉ SUPER_ADMIN gọi được. Tạo tài khoản mới lại nằm ở `EmployeeController` (mục 4.1) vì luôn gắn với 1 nhân viên có sẵn.

| Endpoint | Method | Mô tả |
|---|---|---|
| `/api/employees/{id}/account` | POST | Tạo tài khoản đăng nhập cho 1 nhân viên (permission: `PEOPLE.EMPLOYEE.EDIT`) |
| `/api/employees/{id}/account/reset-password` | PATCH | Admin đặt lại mật khẩu hộ nhân viên (chỉ SUPER_ADMIN) |
| `/api/accounts` | GET | Danh sách toàn bộ tài khoản kèm vai trò đang gán |
| `/api/accounts/{id}/roles` | PUT | **Ghi đè** toàn bộ vai trò của 1 tài khoản |
| `/api/accounts/{id}/copy-roles` | POST | Sao chép toàn bộ vai trò từ 1 tài khoản khác sang tài khoản này |
| `/api/accounts/export/excel` | GET | Xuất danh sách tài khoản ra Excel |

### 4.1. Tạo tài khoản (`createForEmployee`)

- Mỗi Employee **chỉ có tối đa 1 UserAccount** — nhân viên đã có tài khoản thì tạo lần nữa bị chặn (`EMPLOYEE_ALREADY_HAS_ACCOUNT`).
- `username` duy nhất **trong tenant** (`USERNAME_DUPLICATE`).
- Email của tài khoản mặc định lấy theo email công ty của Employee (không nhập riêng).
- Tài khoản mới luôn `status = ACTIVE`, mật khẩu được hash bằng `PasswordEncoder` trước khi lưu — **không bao giờ lưu mật khẩu dạng plaintext**.
- Tài khoản mới **không có vai trò nào** cho tới khi admin gán riêng (mục 3.3/4.2).

### 4.2. Gán vai trò (`assignRoles`)

Hành vi **ghi đè hoàn toàn** danh sách vai trò của tài khoản (xoá hết `user_role` cũ, tạo lại theo danh sách `roleIds` gửi lên). Vai trò không thuộc tenant hiện tại bị lọc bỏ âm thầm.

### 4.3. Sao chép vai trò (`copyRoles`)

Copy **toàn bộ tập vai trò** từ tài khoản nguồn sang tài khoản đích, tái sử dụng đúng `assignRoles()` (nên cũng là **ghi đè**, không cộng dồn vào vai trò sẵn có của tài khoản đích). Cả 2 tài khoản phải cùng tenant. Dùng khi cần cấp nhanh cho 1 nhân viên mới đúng bộ quyền của 1 nhân viên khác đã có sẵn (vd 2 người cùng vị trí).

### 4.4. Đặt lại mật khẩu bởi Admin

Khác với "tự đổi mật khẩu" (mục 2.3, cần biết mật khẩu cũ), endpoint này **chỉ SUPER_ADMIN** gọi được, dùng khi nhân viên quên mật khẩu — không cần biết mật khẩu cũ, chỉ cần tài khoản đó tồn tại (`ACCOUNT_NOT_FOUND` nếu nhân viên chưa có tài khoản).

## 5. Trạng thái (enum) liên quan

| Enum | Giá trị | Ghi chú |
|---|---|---|
| `UserStatus` | `ACTIVE`, `LOCKED`, `DISABLED` | Chỉ `ACTIVE` đăng nhập được (mục 2.1 bước 4) |
| `TenantStatus` | `ACTIVE`, `SUSPENDED` | Chỉ `ACTIVE` đăng nhập được (mục 2.1 bước 2) |

## 6. Dữ liệu khởi tạo (seed)

`DataSeeder` chỉ chạy **1 lần duy nhất** khi tenant `default` chưa tồn tại (kiểm tra trước khi seed, an toàn khi khởi động lại nhiều lần). Tạo sẵn:

- Tenant `default` (`ACTIVE`)
- Quyền wildcard `Permission{code="*"}` — quyền nội bộ đại diện "toàn quyền", không hiển thị trong danh mục UI (`listPermissions()` lọc bỏ `"*"`).
- Vai trò `SUPER_ADMIN` (`systemDefined = true`), được gán quyền `"*"`.
- Tài khoản `admin` / mật khẩu `Admin@123`, gắn vai trò `SUPER_ADMIN`.

> Class này có comment sẵn trong code: **xoá/tắt khi triển khai production thật**, không nên để chạy trên môi trường thật với mật khẩu mặc định này.

## 7. Bảng tổng hợp mã lỗi nghiệp vụ

| Mã lỗi | HTTP | Khi nào |
|---|---|---|
| `TENANT_NOT_FOUND` | 401 | `tenantCode` không tồn tại |
| `TENANT_SUSPENDED` | 401 | Tenant bị tạm ngưng |
| `INVALID_CREDENTIALS` | 401 | Sai username hoặc mật khẩu |
| `USER_NOT_ACTIVE` | 401 | Tài khoản `LOCKED`/`DISABLED` |
| `INVALID_REFRESH_TOKEN` | 401 | Refresh token hết hạn/sai/user không còn tồn tại |
| `INVALID_CURRENT_PASSWORD` | 400 | Đổi mật khẩu nhưng nhập sai mật khẩu hiện tại |
| `ROLE_CODE_DUPLICATE` | 400 | Trùng mã vai trò trong tenant |
| `ROLE_SYSTEM_DEFINED` | 400 | Sửa/xoá/đổi quyền vai trò hệ thống |
| `ROLE_IN_USE` | 400 | Xoá vai trò đang được gán cho tài khoản |
| `ROLE_NOT_FOUND` | 404 | Vai trò không tồn tại / khác tenant |
| `EMPLOYEE_ALREADY_HAS_ACCOUNT` | 400 | Tạo tài khoản cho nhân viên đã có sẵn |
| `USERNAME_DUPLICATE` | 400 | Trùng tên đăng nhập trong tenant |
| `ACCOUNT_NOT_FOUND` | 404 | Tài khoản không tồn tại / khác tenant / nhân viên chưa có tài khoản |
