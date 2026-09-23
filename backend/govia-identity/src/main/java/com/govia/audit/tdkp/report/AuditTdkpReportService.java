package com.govia.audit.tdkp.report;

import com.govia.audit.tdkp.branch.AuditTdkpBranchDto;
import com.govia.audit.tdkp.branch.AuditTdkpBranchService;
import com.govia.audit.tdkp.ceo.AuditTdkpCeoRecommendationDto;
import com.govia.audit.tdkp.ceo.AuditTdkpCeoRecommendationService;
import com.govia.audit.tdkp.ceo.TdkpCeoScope;
import com.govia.audit.tdkp.common.TdkpStatus;
import com.govia.audit.tdkp.common.TdkpSupport;
import com.govia.audit.tdkp.resolution.AuditTdkpResolutionDto;
import com.govia.audit.tdkp.resolution.AuditTdkpResolutionService;
import com.govia.audit.tdkp.unitrec.AuditTdkpUnitRecommendationDto;
import com.govia.audit.tdkp.unitrec.AuditTdkpUnitRecommendationService;
import com.govia.core.web.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Sheet 7 - ZTC_TDKP_BC: dựng dữ liệu 5 báo cáo TDKP (Template_BC_01..05) từ các màn hình nguồn; file Excel do AuditTdkpReportWriter dựng. */
@Service
public class AuditTdkpReportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AuditTdkpCeoRecommendationService ceoService;
    private final AuditTdkpBranchService branchService;
    private final AuditTdkpUnitRecommendationService unitService;
    private final AuditTdkpResolutionService resolutionService;

    public AuditTdkpReportService(AuditTdkpCeoRecommendationService ceoService, AuditTdkpBranchService branchService,
                                  AuditTdkpUnitRecommendationService unitService, AuditTdkpResolutionService resolutionService) {
        this.ceoService = ceoService;
        this.branchService = branchService;
        this.unitService = unitService;
        this.resolutionService = resolutionService;
    }

    @Transactional(readOnly = true)
    public AuditTdkpReportDto.ReportData build(AuditTdkpReportDto.ReportRequest request) {
        LocalDate asOf = request.asOfDate() == null ? LocalDate.now() : request.asOfDate();
        return switch (request.type()) {
            case BC01 -> bc01(request, asOf);
            case BC02 -> bc02(request, asOf);
            case BC03 -> bc03(request, asOf);
            case BC04 -> bc04(request, asOf);
            case BC05 -> bc05(request, asOf);
        };
    }

    @Transactional(readOnly = true)
    public byte[] export(AuditTdkpReportDto.ReportRequest request) {
        return AuditTdkpReportWriter.write(build(request));
    }

    // ===================== BC01: Kien nghi doi voi HDTV/TGD (nguon ZTC_TDKP_CEO_KH) =====================

    private AuditTdkpReportDto.ReportData bc01(AuditTdkpReportDto.ReportRequest request, LocalDate asOf) {
        List<AuditTdkpCeoRecommendationDto.Response> source = ceoService.list(TdkpCeoScope.KH).stream()
                .filter(r -> r.reportDate() == null || !r.reportDate().isAfter(asOf))
                .sorted(Comparator.comparing(AuditTdkpCeoRecommendationDto.Response::reportDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AuditTdkpCeoRecommendationDto.Response::reportNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<List<String>> rows = new ArrayList<>();
        int stt = 1;
        for (AuditTdkpCeoRecommendationDto.Response r : source) {
            rows.add(List.of(String.valueOf(stt++), s(r.reportNumber()), date(r.reportDate()), s(r.content()), s(r.recommendationTypeName()),
                    s(r.businessSegmentCode()), s(r.targetObjectLabel()), s(r.directive()), s(r.executingUnitName()), date(r.deadline()), s(r.statusLabel()),
                    s(r.evaluation()), state(r.deadline(), asOf), s(r.note())));
        }
        // "TC": dem so o co du lieu o cac cot Noi dung KN (4), Linh vuc KN (6), Don vi dau moi (9), Danh gia (12) - nhu COUNTIF(...,"*") cua mau
        List<String> totals = blankRow(14);
        totals.set(0, "TC");
        for (int col : new int[]{3, 5, 8, 11}) {
            totals.set(col, String.valueOf(countFilled(rows, col)));
        }
        return new AuditTdkpReportDto.ReportData(AuditTdkpReportType.BC01, asOf, "(Đến thời điểm " + DATE.format(asOf) + ", ban hành kèm báo cáo số "
                + or(request.attachedReportNumber(), "……../BKS") + " ngày " + dateOr(request.attachedReportDate(), "…../tháng/…./năm") + ")", null,
                List.of("STT", "Số báo cáo", "Ngày báo cáo", "Nội dung kiến nghị", "Phân loại kiến nghị (Cơ chế chính sách/văn bản hướng dẫn/an toàn hoạt động…)",
                        "Lĩnh vực kiến nghị", "Đối tượng được kiến nghị (HĐTV, TGĐ)", "Chỉ đạo của HĐTV, TGĐ", "Đơn vị đầu mối/phối hợp xây dựng trình TGĐ, HĐTV",
                        "Thời hạn hoàn thành", "Hiện trạng (Đã/đang/chưa)", "Đánh giá tình hình thực hiện kiến nghị", "Trạng thái (Trong hạn/quá hạn)", "Ghi Chú"),
                rows, totals, null);
    }

    // ===================== BC02: Kien nghi tai 1 chi nhanh (nguon ZTC_TDKP_CN) =====================

    private AuditTdkpReportDto.ReportData bc02(AuditTdkpReportDto.ReportRequest request, LocalDate asOf) {
        if (request.branchUnitId() == null) {
            throw new BusinessException("TDKP_REPORT_BRANCH_REQUIRED", "Bao cao 02 can chon Chi nhanh");
        }
        List<AuditTdkpBranchDto.RecommendationResponse> recommendations = branchService.listRecommendations().stream()
                .filter(r -> request.branchUnitId().equals(r.auditObjectUnitId()))
                .filter(r -> request.year() == null || request.year().equals(r.auditYear())).toList();
        Map<UUID, List<AuditTdkpBranchDto.DefectResponse>> defects = branchService.listDefects(null).stream()
                .collect(Collectors.groupingBy(AuditTdkpBranchDto.DefectResponse::branchRecommendationId));
        String branchName = recommendations.isEmpty() ? "" : s(recommendations.get(0).branchName());

        List<List<String>> rows = new ArrayList<>();
        int stt = 1;
        int defectTotal = 0;
        int defectDone = 0;
        int recDone = 0;
        for (AuditTdkpBranchDto.RecommendationResponse r : recommendations) {
            List<AuditTdkpBranchDto.DefectResponse> ds = defects.getOrDefault(r.id(), List.of());
            defectTotal += ds.size();
            defectDone += (int) ds.stream().filter(d -> d.customerStatus() == TdkpStatus.DONE).count();
            recDone += r.status() == TdkpStatus.DONE ? 1 : 0;
            if (ds.isEmpty()) {
                rows.add(bc02Row(stt++, r, null, asOf));
            } else {
                for (AuditTdkpBranchDto.DefectResponse d : ds) {
                    rows.add(bc02Row(stt++, r, d, asOf));
                }
            }
        }
        List<String> totals = blankRow(17);
        totals.set(0, "TC");
        for (int col : new int[]{4, 6, 8, 9, 11}) {
            totals.set(col, String.valueOf(countFilled(rows, col)));
        }
        int recTotal = recommendations.size();
        List<List<String>> stats = List.of(
                List.of("Tổng số kiến nghị", String.valueOf(recTotal)),
                List.of("Tổng số tồn tại sai sót liên quan", String.valueOf(defectTotal)),
                List.of("Số kiến nghị đã thực hiện", String.valueOf(recDone), recTotal == 0 ? "" : percent(recDone, recTotal)),
                List.of("Số tồn tại sai sót đã chỉnh sửa", String.valueOf(defectDone), defectTotal == 0 ? "" : percent(defectDone, defectTotal)),
                List.of("Số kiến nghị còn phải chỉnh sửa", String.valueOf(recTotal - recDone)),
                List.of("Số tồn tại sai sót còn phải chỉnh sửa", String.valueOf(defectTotal - defectDone)));
        return new AuditTdkpReportDto.ReportData(AuditTdkpReportType.BC02, asOf, "(Đến thời điểm " + DATE.format(asOf) + ", Theo biên bản kiểm toán ngày "
                + dateOr(request.minutesDate(), "........") + " của đoàn kiểm toán theo quyết định số " + or(request.decisionNumber(), "..../QĐ-BKS") + " ngày "
                + dateOr(request.decisionDate(), "....../tháng....năm....") + " của Trưởng Ban kiểm soát)", branchName,
                List.of("STT", "Chi nhánh", "Mã chi nhánh", "Năm KT", "Nội dung kiến nghị", "Phân loại kiến nghị", "Lĩnh vực kiến nghị",
                        "Sai sót liên quan đến kiến nghị", "Sai sót liên quan đến khách hàng", "Loại sai sót", "Thời hạn hoàn thành kiến nghị", "Nội dung chỉnh sửa",
                        "Thời gian chỉnh sửa", "Hiện trạng (Đã/Đang/Chưa)", "Đánh giá tình hình thực hiện kiến nghị", "Trạng thái (Trong hạn/Quá hạn)", "Ghi Chú"),
                rows, totals, stats);
    }

    private List<String> bc02Row(int stt, AuditTdkpBranchDto.RecommendationResponse r, AuditTdkpBranchDto.DefectResponse d, LocalDate asOf) {
        return List.of(String.valueOf(stt), s(r.branchName()), s(r.branchCode()), r.auditYear() == null ? "" : String.valueOf(r.auditYear()), s(r.content()),
                s(r.recommendationTypeName()), s(r.businessSegmentCode()), d == null ? "" : s(d.defectContent()), d == null ? "" : s(d.customerEntry()),
                d == null ? "" : s(d.defectType()), date(r.deadline()), s(r.editContent()), date(r.lastEditedDate()), s(r.statusLabel()), s(r.evaluation()),
                state(r.deadline(), asOf), s(r.note()));
    }

    // ===================== BC03: Tong hop kien nghi tai cac chi nhanh (nguon ZTC_TDKP_CN) =====================

    private AuditTdkpReportDto.ReportData bc03(AuditTdkpReportDto.ReportRequest request, LocalDate asOf) {
        List<AuditTdkpBranchDto.RecommendationResponse> recommendations = branchService.listRecommendations().stream()
                .filter(r -> request.year() == null || request.year().equals(r.auditYear()))
                .filter(r -> request.branchUnitId() == null || request.branchUnitId().equals(r.auditObjectUnitId()))
                .sorted(Comparator.comparing(AuditTdkpBranchDto.RecommendationResponse::branchName, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AuditTdkpBranchDto.RecommendationResponse::managementCode)).toList();
        List<List<String>> rows = new ArrayList<>();
        int[] sums = new int[6];
        int stt = 1;
        for (AuditTdkpBranchDto.RecommendationResponse r : recommendations) {
            boolean done = r.status() == TdkpStatus.DONE;
            int[] values = {1, r.defectCount(), done ? 1 : 0, r.defectDoneCount(), done ? 0 : 1, r.defectCount() - r.defectDoneCount()};
            for (int i = 0; i < values.length; i++) {
                sums[i] += values[i];
            }
            rows.add(List.of(String.valueOf(stt++), s(r.branchName()), s(r.branchCode()), r.auditYear() == null ? "" : String.valueOf(r.auditYear()), s(r.content()),
                    s(r.recommendationTypeName()), s(r.businessSegmentCode()), String.valueOf(values[0]), String.valueOf(values[1]), String.valueOf(values[2]),
                    String.valueOf(values[3]), String.valueOf(values[4]), String.valueOf(values[5]), s(r.note())));
        }
        List<String> totals = blankRow(14);
        totals.set(0, "Tổng cộng");
        for (int i = 0; i < sums.length; i++) {
            totals.set(7 + i, String.valueOf(sums[i]));
        }
        return new AuditTdkpReportDto.ReportData(AuditTdkpReportType.BC03, asOf, "(Đến thời điểm " + DATE.format(asOf) + ", Ban hành kèm báo cáo "
                + or(request.attachedReportNumber(), ".......") + ")", null,
                List.of("STT", "Chi nhánh", "Mã chi nhánh", "Năm KT", "Nội dung kiến nghị", "Phân loại kiến nghị", "Lĩnh vực kiến nghị (QTĐH, TD, HĐV…)",
                        "Số kiến nghị", "Số tồn tại sai sót liên quan", "Thực hiện chỉnh sửa trong kỳ - Số kiến nghị", "Thực hiện chỉnh sửa trong kỳ - Số tồn tại sai sót liên quan",
                        "Số kiến nghị còn phải thực hiện - Số kiến nghị", "Số kiến nghị còn phải thực hiện - Số tồn tại sai sót liên quan", "Ghi Chú"),
                rows, totals, null);
    }

    // ===================== BC04: Kien nghi cua don vi doi voi KTNB (nguon ZTC_TDKP_KTNB) =====================

    private AuditTdkpReportDto.ReportData bc04(AuditTdkpReportDto.ReportRequest request, LocalDate asOf) {
        List<AuditTdkpUnitRecommendationDto.Response> source = unitService.list().stream()
                .filter(r -> r.reportDate() == null || !r.reportDate().isAfter(asOf))
                .sorted(Comparator.comparing(AuditTdkpUnitRecommendationDto.Response::reportDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AuditTdkpUnitRecommendationDto.Response::code)).toList();
        List<List<String>> rows = new ArrayList<>();
        int stt = 1;
        for (AuditTdkpUnitRecommendationDto.Response r : source) {
            rows.add(List.of(String.valueOf(stt++), s(r.reportNumber()), date(r.reportDate()), s(r.unitName()), s(r.content()), date(r.deadline()),
                    s(r.implementation()), s(r.statusLabel()), s(r.evaluation()), state(r.deadline(), asOf), s(r.note())));
        }
        List<String> totals = blankRow(11);
        totals.set(0, "TC");
        totals.set(4, String.valueOf(countFilled(rows, 4)));
        totals.set(8, String.valueOf(countFilled(rows, 8)));
        return new AuditTdkpReportDto.ReportData(AuditTdkpReportType.BC04, asOf, "(Đến thời điểm " + DATE.format(asOf) + ", ban hành kèm báo cáo số "
                + or(request.attachedReportNumber(), "……../BKS") + " ngày " + dateOr(request.attachedReportDate(), "…../tháng/…./năm") + ")", null,
                List.of("STT", "Số báo cáo", "Ngày báo cáo", "Đơn vị kiến nghị", "Nội dung kiến nghị", "Thời hạn hoàn thành", "Tình hình thực hiện kiến nghị",
                        "Hiện trạng (đã/đang/chưa)", "Đánh giá tình hình thực hiện kiến nghị", "Trạng thái (trong hạn/quá hạn)", "Ghi Chú"),
                rows, totals, null);
    }

    // ===================== BC05: Theo doi thuc hien nghi quyet HDTV (nguon ZTC_TDKP_NQ) =====================

    private AuditTdkpReportDto.ReportData bc05(AuditTdkpReportDto.ReportRequest request, LocalDate asOf) {
        List<AuditTdkpResolutionDto.Response> source = resolutionService.list().stream()
                .filter(r -> request.year() == null || (r.issueDate() != null && r.issueDate().getYear() == request.year()))
                .filter(r -> r.issueDate() == null || !r.issueDate().isAfter(asOf))
                .sorted(Comparator.comparing(AuditTdkpResolutionDto.Response::issueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AuditTdkpResolutionDto.Response::resolutionNumber)).toList();
        // "NGƯỜI GIÁM SÁT" cua bao cao goc khop voi field "Nguoi theo doi" hien co (cot "Nguoi giam sat"
        // rieng da bi bo khoi man hinh theo phan hoi nguoi dung o test23.9 - xem AuditTdkpResolution).
        List<List<String>> rows = new ArrayList<>();
        for (AuditTdkpResolutionDto.Response r : source) {
            rows.add(List.of(s(r.resolutionNumber()), date(r.issueDate()), s(r.content()), s(r.implementation()), s(r.progressStatusLabel()), s(r.followerName()),
                    s(r.issuanceEvaluation()), s(r.note())));
        }
        return new AuditTdkpReportDto.ReportData(AuditTdkpReportType.BC05, asOf, null, request.year() == null ? String.valueOf(asOf.getYear()) : String.valueOf(request.year()),
                List.of("SỐ NQ", "NGÀY BAN HÀNH", "NỘI DUNG NGHỊ QUYẾT", "TÌNH HÌNH THỰC HIỆN", "ĐÁNH GIÁ (Đã/Đang/Chưa thực hiện)", "NGƯỜI GIÁM SÁT",
                        "ĐÁNH GIÁ VỀ VIỆC BAN HÀNH NGHỊ QUYẾT CỦA HĐTV", "Ghi chú"),
                rows, null, null);
    }

    // ===================== tien ich =====================

    private static String s(String value) {
        return value == null ? "" : value;
    }

    private static String or(String value, String fallback) {
        return TdkpSupport.isBlank(value) ? fallback : value.trim();
    }

    private static String date(LocalDate value) {
        return value == null ? "" : DATE.format(value);
    }

    private static String dateOr(LocalDate value, String fallback) {
        return value == null ? fallback : DATE.format(value);
    }

    private static String state(LocalDate deadline, LocalDate asOf) {
        return s(TdkpSupport.deadlineLabel(TdkpSupport.deadlineState(deadline, asOf)));
    }

    private static String percent(int part, int total) {
        return String.format(java.util.Locale.ROOT, "%.1f%%", part * 100.0 / total);
    }

    private static List<String> blankRow(int size) {
        List<String> row = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            row.add("");
        }
        return row;
    }

    private static int countFilled(List<List<String>> rows, int col) {
        return (int) rows.stream().filter(r -> !r.get(col).isBlank()).count();
    }
}
