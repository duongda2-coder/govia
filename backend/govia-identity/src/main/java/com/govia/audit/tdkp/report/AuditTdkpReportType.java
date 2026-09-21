package com.govia.audit.tdkp.report;

/** 5 báo cáo của màn hình ZTC_TDKP_BC (Template_BC_01 ... Template_BC_05). */
public enum AuditTdkpReportType {
    BC01("Tổng hợp tình hình thực hiện kiến nghị của KTNB đối với HĐTV và TGĐ"),
    BC02("Báo cáo tình hình thực hiện kiến nghị của KTNB tại Agribank Chi nhánh"),
    BC03("Tổng hợp tình hình thực hiện kiến nghị của KTNB tại các Chi nhánh được KTNB"),
    BC04("Tổng hợp tình hình thực hiện kiến nghị của các đơn vị, bộ phận đối với KTNB"),
    BC05("Bảng tổng hợp theo dõi tình hình thực hiện nghị quyết Hội đồng thành viên");

    private final String title;

    AuditTdkpReportType(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
