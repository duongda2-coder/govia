package com.govia.audit.planengagement.recommendation.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * "Lưu mã kiến nghị" (Khối C, chức năng "3. Thêm kiến nghị") - catalog kiến nghị dùng riêng cho
 * TỪNG cuộc kiểm toán (engagementId). Luôn có sẵn 1 dòng mặc định "KNKT000"/"Kiến nghị chung" cho
 * mỗi engagement (xem AuditRecommendationService.list - tự seed nếu rỗng), người dùng thêm các mã
 * tiếp theo KNKT001, KNKT002...
 */
@Getter
@Setter
@Entity
@Table(name = "audit_recommendation")
public class AuditRecommendation extends BaseEntity {

    @Column(name = "engagement_id", nullable = false, columnDefinition = "uuid")
    private UUID engagementId;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    /** "Loại Nghiệp vụ" - tham chiếu AuditMasterDataItem category BUSINESS_SEGMENT. */
    @Column(name = "business_segment_id", columnDefinition = "uuid")
    private UUID businessSegmentId;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;
}
