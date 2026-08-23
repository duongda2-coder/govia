#!/usr/bin/env bash
# Seed du lieu demo de test quy trinh phe duyet nhan vien (employee_approval):
#   Trung tam Kiem toan > Phong Kiem toan 1 > Bo phan Kiem toan 1 (+ Ban Thanh tra doc lap)
# Moi don vi co 1 truong don vi + vai nhan vien. 3 truong don vi (Ong A/B/C trong chuoi quan ly)
# duoc tao tai khoan dang nhap + gan quyen duyet task workflow.
#
# Chay: bash seed-audit-org-demo.sh
# Script se HOI ban nhap tenant/username/password ngay tren terminal - KHONG luu, KHONG gui di dau ca.

set -euo pipefail

read -rp "Base URL backend [http://localhost:8081]: " BASE_URL
BASE_URL=${BASE_URL:-http://localhost:8081}
read -rp "Ma tenant [default]: " TENANT
TENANT=${TENANT:-default}
read -rp "Username dang nhap (co quyen SUPER_ADMIN): " ADMIN_USER
read -rsp "Password: " ADMIN_PASS
echo

field() { grep -o "\"$1\":\"[^\"]*\"" | head -1 | sed -E "s/.*\"$1\":\"([^\"]*)\".*/\1/"; }
field_raw() { grep -o "\"$1\":[^,}]*" | head -1 | sed -E "s/.*\"$1\":([^,}]*)/\1/"; }

echo "== Dang nhap =="
LOGIN_RESP=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"tenantCode\":\"$TENANT\",\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}")
TOKEN=$(echo "$LOGIN_RESP" | field accessToken)
if [ -z "$TOKEN" ]; then
  echo "Dang nhap that bai. Phan hoi server:"
  echo "$LOGIN_RESP"
  exit 1
fi
echo "Dang nhap OK."

auth() { curl -s -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" "$@"; }

create_org_unit() {
  local code="$1" name="$2" level="$3" parent="$4"
  local parent_json="null"
  [ -n "$parent" ] && parent_json="\"$parent\""
  local resp
  resp=$(auth -X POST "$BASE_URL/api/org-units" -d "{\"code\":\"$code\",\"name\":\"$name\",\"type\":null,\"levelCode\":\"$level\",\"parentId\":$parent_json,\"managerEmployeeId\":null}")
  echo "$resp" | field id
}

create_employee() {
  local code="$1" name="$2" org="$3" manager="$4"
  local org_json="null" mgr_json="null"
  [ -n "$org" ] && org_json="\"$org\""
  [ -n "$manager" ] && mgr_json="\"$manager\""
  local resp
  resp=$(auth -X POST "$BASE_URL/api/employees" \
    -d "{\"employeeCode\":\"$code\",\"fullName\":\"$name\",\"email\":null,\"personalEmail\":null,\"phone\":null,\"orgUnitId\":$org_json,\"positionId\":null,\"hireDate\":null,\"dateOfBirth\":null,\"gender\":null,\"idNumber\":null,\"managerId\":$mgr_json}")
  echo "$resp" | field id
}

create_account() {
  local employee_id="$1" username="$2" password="$3"
  auth -X POST "$BASE_URL/api/employees/$employee_id/account" \
    -d "{\"username\":\"$username\",\"password\":\"$password\"}" > /dev/null
}

account_id_for_employee() {
  local employee_id="$1"
  auth "$BASE_URL/api/accounts" | grep -o "{[^}]*\"employeeId\":\"$employee_id\"[^}]*}" | head -1 | field id
}

echo "== Tao don vi to chuc =="
TTKT_ID=$(create_org_unit "TTKT" "Trung tam Kiem toan" "002" "")
echo "  Trung tam Kiem toan: $TTKT_ID"
PKT1_ID=$(create_org_unit "PKT1" "Phong Kiem toan 1" "003" "$TTKT_ID")
echo "  Phong Kiem toan 1: $PKT1_ID"
BPKT1_ID=$(create_org_unit "BPKT1" "Bo phan Kiem toan 1" "004" "$PKT1_ID")
echo "  Bo phan Kiem toan 1: $BPKT1_ID"
BTT_ID=$(create_org_unit "BTT" "Ban Thanh tra" "002" "")
echo "  Ban Thanh tra: $BTT_ID"

echo "== Tao nhan vien (truong don vi + nhan vien) =="
# Ong C: Truong Trung tam Kiem toan - dinh cao chuoi quan ly, khong can quan ly rieng
ONG_C=$(create_employee "NV-TTKT-01" "Tran Van C (Truong Trung tam Kiem toan)" "$TTKT_ID" "")
echo "  Ong C - Truong Trung tam Kiem toan: $ONG_C"

# Ong B: Truong Phong Kiem toan 1, bao cao cho Ong C
ONG_B=$(create_employee "NV-PKT1-01" "Le Van B (Truong Phong Kiem toan 1)" "$PKT1_ID" "$ONG_C")
echo "  Ong B - Truong Phong Kiem toan 1: $ONG_B"

# Ong A: Truong Bo phan Kiem toan 1, bao cao cho Ong B - day la nguoi se duoc chon lam "Quan ly"
# khi tao nhan vien moi de kich hoat day duyet 3 cap (A -> B -> C -> Super Admin).
ONG_A=$(create_employee "NV-BPKT1-01" "Pham Van A (Truong Bo phan Kiem toan 1)" "$BPKT1_ID" "$ONG_B")
echo "  Ong A - Truong Bo phan Kiem toan 1 (chon nguoi nay lam Quan ly khi test): $ONG_A"

# Vai nhan vien thuong moi don vi
create_employee "NV-TTKT-02" "Nguyen Thi Trung 2" "$TTKT_ID" "$ONG_C" > /dev/null
create_employee "NV-PKT1-02" "Nguyen Thi Phong 2" "$PKT1_ID" "$ONG_B" > /dev/null
create_employee "NV-BPKT1-02" "Nguyen Thi Bo 2" "$BPKT1_ID" "$ONG_A" > /dev/null
create_employee "NV-BPKT1-03" "Nguyen Thi Bo 3" "$BPKT1_ID" "$ONG_A" > /dev/null

BTT_HEAD=$(create_employee "NV-BTT-01" "Hoang Van D (Truong Ban Thanh tra)" "$BTT_ID" "")
create_employee "NV-BTT-02" "Nguyen Thi Thanh 2" "$BTT_ID" "$BTT_HEAD" > /dev/null
echo "  Da tao them nhan vien thuong cho Trung tam/Phong/Bo phan/Ban Thanh tra."

echo "== Tao role WORKFLOW_APPROVER (WORKFLOW.TASK.VIEW + WORKFLOW.TASK.COMPLETE) =="
ROLE_ID=$(auth "$BASE_URL/api/roles" | grep -o "{[^}]*\"code\":\"WORKFLOW_APPROVER\"[^}]*}" | head -1 | field id)
if [ -z "$ROLE_ID" ]; then
  ROLE_ID=$(auth -X POST "$BASE_URL/api/roles" -d '{"code":"WORKFLOW_APPROVER","name":"Nguoi duyet quy trinh","description":"Xem va hoan tat task workflow"}' | field id)
  auth -X PUT "$BASE_URL/api/roles/$ROLE_ID/permissions" -d '{"permissionCodes":["WORKFLOW.TASK.VIEW","WORKFLOW.TASK.COMPLETE"]}' > /dev/null
  echo "  Da tao role moi: $ROLE_ID"
else
  echo "  Role da ton tai: $ROLE_ID"
fi

echo "== Tao tai khoan dang nhap cho Ong A / B / C =="
DEMO_PASSWORD="Demo@12345"
for pair in "$ONG_A:ong-a" "$ONG_B:ong-b" "$ONG_C:ong-c"; do
  emp_id="${pair%%:*}"
  username="${pair##*:}"
  create_account "$emp_id" "$username" "$DEMO_PASSWORD"
  acc_id=$(account_id_for_employee "$emp_id")
  auth -X PUT "$BASE_URL/api/accounts/$acc_id/roles" -d "{\"roleIds\":[\"$ROLE_ID\"]}" > /dev/null
  echo "  $username / $DEMO_PASSWORD  (employee=$emp_id, account=$acc_id)"
done

cat <<EOF

============================================================
XONG! Da tao xong du lieu mau.

Tai khoan de dang nhap thu vai tro quan ly (password chung: $DEMO_PASSWORD):
  ong-a  -> Pham Van A, Truong Bo phan Kiem toan 1 (cap duyet 1)
  ong-b  -> Le Van B, Truong Phong Kiem toan 1     (cap duyet 2)
  ong-c  -> Tran Van C, Truong Trung tam Kiem toan  (cap duyet 3)

De test quy trinh phe duyet:
  1. Dang nhap admin, vao "Nhan vien" > "Them moi".
  2. Dat "Quan ly truc tiep" = Pham Van A (Truong Bo phan Kiem toan 1).
  3. Bam Luu -> nhan vien se o trang thai "Cho phe duyet".
  4. Dang nhap lai bang ong-a -> vao "Quy trinh" > "Viec can xu ly" -> Duyet.
  5. Lam tuong tu voi ong-b, ong-c.
  6. Dang nhap lai admin -> "Viec can xu ly" -> Nhan viec -> Duyet (buoc Super Admin).
  7. Nhan vien moi chuyen sang "Dang lam viec".
============================================================
EOF
