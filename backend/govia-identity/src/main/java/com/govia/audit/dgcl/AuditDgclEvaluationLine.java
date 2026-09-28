package com.govia.audit.dgcl;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Gia tri nhap tay tren 1 dong tieu chi cua phieu - noi dung tieu chi KHONG luu o day ma lay tu bo
 * tieu chi co dinh {@link DgclCriteriaCatalog} theo itemKey. */
@Getter
@Setter
@Entity
@Table(name = "audit_dgcl_evaluation_line")
public class AuditDgclEvaluationLine extends BaseEntity {

    @Column(name = "evaluation_id", nullable = false, columnDefinition = "uuid")
    private UUID evaluationId;

    @Column(name = "item_key", nullable = false, length = 10)
    private String itemKey;

    /** PL01A/PL01B: "Nội dung thực hiện" (NDTH). */
    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "compliant", nullable = false)
    private boolean compliant;

    @Column(name = "non_compliant", nullable = false)
    private boolean nonCompliant;

    /** PL01F: tich diem cong/diem tru. */
    @Column(name = "checked", nullable = false)
    private boolean checked;

    /** PL01F: "Số lượng lỗi vi phạm" cua dong diem tru chat luong. */
    @Column(name = "violation_count")
    private Integer violationCount;

    /** PL01F: "Điểm tối đa" nhap tay cua dong I/II/III (mac dinh 100). */
    @Column(name = "max_score", precision = 9, scale = 4)
    private BigDecimal maxScore;

    @Column(name = "detail", length = 2000)
    private String detail;

    @Column(name = "document", length = 1000)
    private String document;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "evaluator_name")
    private String evaluatorName;
}
