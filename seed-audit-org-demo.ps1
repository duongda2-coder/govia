# Seed du lieu demo de test quy trinh phe duyet nhan vien (employee_approval):
#   Trung tam Kiem toan > Phong Kiem toan 1 > Bo phan Kiem toan 1 (+ Ban Thanh tra doc lap)
# Moi don vi co 1 truong don vi + vai nhan vien. 3 truong don vi (Ong A/B/C trong chuoi quan ly)
# duoc tao tai khoan dang nhap + gan quyen duyet task workflow.
#
# Chay: powershell -ExecutionPolicy Bypass -File seed-audit-org-demo.ps1
# Script se HOI ban nhap tenant/username/password ngay tren terminal cua CHINH BAN - khong gui di dau.

$ErrorActionPreference = "Stop"

$BaseUrl = Read-Host "Base URL backend [http://localhost:8081]"
if ([string]::IsNullOrWhiteSpace($BaseUrl)) { $BaseUrl = "http://localhost:8081" }
$Tenant = Read-Host "Ma tenant [default]"
if ([string]::IsNullOrWhiteSpace($Tenant)) { $Tenant = "default" }
$AdminUser = Read-Host "Username dang nhap (co quyen SUPER_ADMIN)"
$AdminPassSecure = Read-Host "Password" -AsSecureString
$AdminPass = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto([System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($AdminPassSecure))

Write-Host "== Dang nhap =="
$loginBody = @{ tenantCode = $Tenant; username = $AdminUser; password = $AdminPass } | ConvertTo-Json
try {
    $loginResp = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
} catch {
    Write-Host "Dang nhap that bai: $($_.Exception.Message)"
    exit 1
}
$Token = $loginResp.data.accessToken
if (-not $Token) {
    Write-Host "Dang nhap that bai - khong nhan duoc accessToken."
    exit 1
}
Write-Host "Dang nhap OK."
$Headers = @{ Authorization = "Bearer $Token" }

function New-DemoOrgUnit($code, $name, $level, $parentId) {
    $body = @{ code = $code; name = $name; type = $null; levelCode = $level; parentId = $parentId; managerEmployeeId = $null } | ConvertTo-Json
    $resp = Invoke-RestMethod -Uri "$BaseUrl/api/org-units" -Method Post -Headers $Headers -ContentType "application/json" -Body $body
    return $resp.data.id
}

function New-DemoEmployee($code, $name, $orgId, $managerId) {
    $body = @{
        employeeCode = $code; fullName = $name; email = $null; personalEmail = $null; phone = $null
        orgUnitId = $orgId; positionId = $null; hireDate = $null; dateOfBirth = $null; gender = $null
        idNumber = $null; managerId = $managerId
    } | ConvertTo-Json
    $resp = Invoke-RestMethod -Uri "$BaseUrl/api/employees" -Method Post -Headers $Headers -ContentType "application/json" -Body $body
    return $resp.data.id
}

function New-DemoAccount($employeeId, $username, $password) {
    $body = @{ username = $username; password = $password } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/employees/$employeeId/account" -Method Post -Headers $Headers -ContentType "application/json" -Body $body | Out-Null
}

function Get-AccountIdForEmployee($employeeId) {
    $resp = Invoke-RestMethod -Uri "$BaseUrl/api/accounts" -Headers $Headers
    $acc = $resp.data | Where-Object { $_.employeeId -eq $employeeId }
    return $acc.id
}

Write-Host "== Tao don vi to chuc =="
$TtktId = New-DemoOrgUnit "TTKT" "Trung tam Kiem toan" "002" $null
Write-Host "  Trung tam Kiem toan: $TtktId"
$Pkt1Id = New-DemoOrgUnit "PKT1" "Phong Kiem toan 1" "003" $TtktId
Write-Host "  Phong Kiem toan 1: $Pkt1Id"
$Bpkt1Id = New-DemoOrgUnit "BPKT1" "Bo phan Kiem toan 1" "004" $Pkt1Id
Write-Host "  Bo phan Kiem toan 1: $Bpkt1Id"
$BttId = New-DemoOrgUnit "BTT" "Ban Thanh tra" "002" $null
Write-Host "  Ban Thanh tra: $BttId"

Write-Host "== Tao nhan vien (truong don vi + nhan vien) =="
$OngC = New-DemoEmployee "NV-TTKT-01" "Tran Van C (Truong Trung tam Kiem toan)" $TtktId $null
Write-Host "  Ong C - Truong Trung tam Kiem toan: $OngC"
$OngB = New-DemoEmployee "NV-PKT1-01" "Le Van B (Truong Phong Kiem toan 1)" $Pkt1Id $OngC
Write-Host "  Ong B - Truong Phong Kiem toan 1: $OngB"
$OngA = New-DemoEmployee "NV-BPKT1-01" "Pham Van A (Truong Bo phan Kiem toan 1)" $Bpkt1Id $OngB
Write-Host "  Ong A - Truong Bo phan Kiem toan 1 (chon nguoi nay lam Quan ly khi test): $OngA"

New-DemoEmployee "NV-TTKT-02" "Nguyen Thi Trung 2" $TtktId $OngC | Out-Null
New-DemoEmployee "NV-PKT1-02" "Nguyen Thi Phong 2" $Pkt1Id $OngB | Out-Null
New-DemoEmployee "NV-BPKT1-02" "Nguyen Thi Bo 2" $Bpkt1Id $OngA | Out-Null
New-DemoEmployee "NV-BPKT1-03" "Nguyen Thi Bo 3" $Bpkt1Id $OngA | Out-Null
$BttHead = New-DemoEmployee "NV-BTT-01" "Hoang Van D (Truong Ban Thanh tra)" $BttId $null
New-DemoEmployee "NV-BTT-02" "Nguyen Thi Thanh 2" $BttId $BttHead | Out-Null
Write-Host "  Da tao them nhan vien thuong cho Trung tam/Phong/Bo phan/Ban Thanh tra."

Write-Host "== Tao role WORKFLOW_APPROVER (WORKFLOW.TASK.VIEW + WORKFLOW.TASK.COMPLETE) =="
$rolesResp = Invoke-RestMethod -Uri "$BaseUrl/api/roles" -Headers $Headers
$existingRole = $rolesResp.data | Where-Object { $_.code -eq "WORKFLOW_APPROVER" }
if ($existingRole) {
    $RoleId = $existingRole.id
    Write-Host "  Role da ton tai: $RoleId"
} else {
    $roleBody = @{ code = "WORKFLOW_APPROVER"; name = "Nguoi duyet quy trinh"; description = "Xem va hoan tat task workflow" } | ConvertTo-Json
    $roleResp = Invoke-RestMethod -Uri "$BaseUrl/api/roles" -Method Post -Headers $Headers -ContentType "application/json" -Body $roleBody
    $RoleId = $roleResp.data.id
    $permBody = @{ permissionCodes = @("WORKFLOW.TASK.VIEW", "WORKFLOW.TASK.COMPLETE") } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/roles/$RoleId/permissions" -Method Put -Headers $Headers -ContentType "application/json" -Body $permBody | Out-Null
    Write-Host "  Da tao role moi: $RoleId"
}

Write-Host "== Tao tai khoan dang nhap cho Ong A / B / C =="
$DemoPassword = "Demo@12345"
$Managers = @(
    [pscustomobject]@{ Id = $OngA; Username = "ong-a" },
    [pscustomobject]@{ Id = $OngB; Username = "ong-b" },
    [pscustomobject]@{ Id = $OngC; Username = "ong-c" }
)
foreach ($m in $Managers) {
    New-DemoAccount $m.Id $m.Username $DemoPassword
    $accId = Get-AccountIdForEmployee $m.Id
    $assignBody = @{ roleIds = @($RoleId) } | ConvertTo-Json
    Invoke-RestMethod -Uri "$BaseUrl/api/accounts/$accId/roles" -Method Put -Headers $Headers -ContentType "application/json" -Body $assignBody | Out-Null
    Write-Host "  $($m.Username) / $DemoPassword  (employee=$($m.Id), account=$accId)"
}

Write-Host ""
Write-Host "============================================================"
Write-Host "XONG! Da tao xong du lieu mau."
Write-Host ""
Write-Host "Tai khoan de dang nhap thu vai tro quan ly (password chung: $DemoPassword):"
Write-Host "  ong-a  -> Pham Van A, Truong Bo phan Kiem toan 1 (cap duyet 1)"
Write-Host "  ong-b  -> Le Van B, Truong Phong Kiem toan 1     (cap duyet 2)"
Write-Host "  ong-c  -> Tran Van C, Truong Trung tam Kiem toan  (cap duyet 3)"
Write-Host ""
Write-Host "De test quy trinh phe duyet:"
Write-Host "  1. Dang nhap admin, vao 'Nhan vien' > 'Them moi'."
Write-Host "  2. Dat 'Quan ly truc tiep' = Pham Van A (Truong Bo phan Kiem toan 1)."
Write-Host "  3. Bam Luu -> nhan vien se o trang thai 'Cho phe duyet'."
Write-Host "  4. Dang nhap lai bang ong-a -> vao 'Quy trinh' > 'Viec can xu ly' -> Duyet."
Write-Host "  5. Lam tuong tu voi ong-b, ong-c."
Write-Host "  6. Dang nhap lai admin -> 'Viec can xu ly' -> Nhan viec -> Duyet (buoc Super Admin)."
Write-Host "  7. Nhan vien moi chuyen sang 'Dang lam viec'."
Write-Host "============================================================"
