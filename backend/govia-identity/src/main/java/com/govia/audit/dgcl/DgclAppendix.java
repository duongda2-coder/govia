package com.govia.audit.dgcl;

/** 3 phu luc cham diem cua man hinh "Đánh giá chất lượng" (DGCL_CN.xlsx, sheet PL 01A/PL 01B/PL 01F). */
public enum DgclAppendix {
    /** Kiem soat tuan thu trinh tu, thoi gian thuc hien CKT - diem = Tuan thu / NDTH. */
    PL01A,
    /** Kiem soat chat luong noi dung cuoc kiem toan - diem = Tuan thu / NDTH. */
    PL01B,
    /** Danh gia chat luong cuoc kiem toan - tong hop B1 (PL01A), B2 (PL01B), B3 (diem tru chat luong), diem cong/tru. */
    PL01F
}
