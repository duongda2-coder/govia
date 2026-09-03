package com.govia.audit.employeecapability.entity;

import com.govia.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Man hinh "Khai bao kha nang dam nhan linh vuc cua nhan vien" (sheet ZTC_KNDN, bang ZTB_KNDN) -
 * 1 dong duy nhat cho moi Employee (khong tao/xoa thu cong, danh sach luon lay TAT CA nhan vien tu
 * danh muc Nhan vien sang - xem AuditEmployeeCapabilityService.list()). 14 co "kha nang dam nhan"
 * gom 10 linh vuc nghiep vu (The, QTDH, HDV, TCKT, CNTT, TTKQ, PCRT, TTQT, XDCB, TD) va 4 vai tro
 * trong doan kiem toan (Truong doan, Truong nhom, To giam sat, Thuc hien DGCL).
 */
@Getter
@Setter
@Entity
@Table(name = "audit_employee_capability")
public class AuditEmployeeCapability extends BaseEntity {

    @Column(name = "employee_id", nullable = false, columnDefinition = "uuid")
    private UUID employeeId;

    /** The - san pham/dich vu the ngan hang. */
    @Column(name = "the_capable", nullable = false)
    private boolean theCapable;

    /** QTDH - Quan tri dieu hanh. */
    @Column(name = "qtdh_capable", nullable = false)
    private boolean qtdhCapable;

    /** HDV - Huy dong von. */
    @Column(name = "hdv_capable", nullable = false)
    private boolean hdvCapable;

    /** TCKT - Tai chinh ke toan. */
    @Column(name = "tckt_capable", nullable = false)
    private boolean tcktCapable;

    /** CNTT - Cong nghe thong tin. */
    @Column(name = "cntt_capable", nullable = false)
    private boolean cnttCapable;

    /** TTKQ - Tien te kho quy. */
    @Column(name = "ttkq_capable", nullable = false)
    private boolean ttkqCapable;

    /** PCRT - Phong chong rua tien. */
    @Column(name = "pcrt_capable", nullable = false)
    private boolean pcrtCapable;

    /** TTQT - Thanh toan quoc te. */
    @Column(name = "ttqt_capable", nullable = false)
    private boolean ttqtCapable;

    /** XDCB - Xay dung co ban. */
    @Column(name = "xdcb_capable", nullable = false)
    private boolean xdcbCapable;

    /** TD - Tin dung. */
    @Column(name = "td_capable", nullable = false)
    private boolean tdCapable;

    @Column(name = "truong_doan_capable", nullable = false)
    private boolean truongDoanCapable;

    @Column(name = "truong_nhom_capable", nullable = false)
    private boolean truongNhomCapable;

    @Column(name = "to_giam_sat_capable", nullable = false)
    private boolean toGiamSatCapable;

    /** DGCL - Danh gia chat luong. */
    @Column(name = "dgcl_capable", nullable = false)
    private boolean dgclCapable;

    @Column(name = "approved", nullable = false)
    private boolean approved;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;
}
