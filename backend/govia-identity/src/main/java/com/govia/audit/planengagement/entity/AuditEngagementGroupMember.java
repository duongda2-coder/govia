package com.govia.audit.planengagement.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Thanh vien cua 1 nhom (man hinh "Danh sach thanh vien trong nhom") - toi da 3 nghiep vu duoc
 * giao cho thanh vien nay, dung de tu dong phan cong cong viec kiem toan tuong ung. */
@Getter
@Setter
@Entity
@Table(name = "audit_engagement_group_member")
public class AuditEngagementGroupMember extends BaseEntity {

    @Column(name = "group_id", nullable = false, columnDefinition = "uuid")
    private UUID groupId;

    @Column(name = "employee_id", nullable = false, columnDefinition = "uuid")
    private UUID employeeId;

    /** "Nghiep vu 1/2/3" - link toi AuditMasterDataItem danh muc BUSINESS_SEGMENT, toi da 3, co the bo trong. */
    @Column(name = "business_segment_1_id", columnDefinition = "uuid")
    private UUID businessSegment1Id;

    @Column(name = "business_segment_2_id", columnDefinition = "uuid")
    private UUID businessSegment2Id;

    @Column(name = "business_segment_3_id", columnDefinition = "uuid")
    private UUID businessSegment3Id;
}
