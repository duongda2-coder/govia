package com.govia.identity.entity;

/**
 * Cap bac nhan su (doc lap voi don vi to chuc) - dung lam nguong dung cho dây phe duyet dong:
 * di nguoc chuoi quan ly (manager_id) toi khi gap nguoi co cap N >= nguong cau hinh trong
 * ApprovalMatrixRule.finalApprovalLevel (xem EmployeeApprovalService).
 */
public enum EmployeeRankLevel {
    N1, N2, N3, N4, N5, N6
}
