package com.govia.identity;

import com.govia.audit.dgcl.AuditDgclDto.LineInput;
import com.govia.audit.dgcl.AuditDgclDto.SaveRequest;
import com.govia.audit.dgcl.AuditDgclDto.Sheet;
import com.govia.audit.dgcl.AuditDgclDto.SubjectRow;
import com.govia.audit.dgcl.AuditDgclService;
import com.govia.audit.dgcl.DgclAppendix;
import com.govia.audit.dgcl.DgclCriteriaCatalog;
import com.govia.audit.dgcl.DgclScoring;
import com.govia.audit.dgcl.DgclScoring.LineValues;
import com.govia.audit.dgcl.DgclScoring.QualityResult;
import com.govia.audit.employeecapability.dto.AuditEmployeeCapabilityItemRequest;
import com.govia.audit.employeecapability.service.AuditEmployeeCapabilityService;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.audit.planengagement.entity.AuditEngagement;
import com.govia.audit.planengagement.entity.AuditEngagementGroup;
import com.govia.audit.planengagement.entity.AuditEngagementGroupMember;
import com.govia.audit.planengagement.repository.AuditEngagementGroupMemberRepository;
import com.govia.audit.planengagement.repository.AuditEngagementGroupRepository;
import com.govia.audit.planengagement.repository.AuditEngagementRepository;
import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.core.security.CurrentUserPrincipal;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeResponse;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.service.EmployeeService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** Kiem chung phan he "Danh gia chat luong" (DGCL_CN.xlsx): cong thuc PL01A/B/F, xac nhan/huy xac nhan, kiem soat khoa
 * phieu, quyen theo 2 cot KNDN, xuat PL04B1. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditDgclTest {

    @Autowired private AuditDgclService service;
    @Autowired private DgclCriteriaCatalog catalog;
    @Autowired private AuditEmployeeCapabilityService capabilityService;
    @Autowired private EmployeeService employeeService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private AuditObjectUnitRepository unitRepository;
    @Autowired private AuditEngagementRepository engagementRepository;
    @Autowired private AuditEngagementGroupRepository groupRepository;
    @Autowired private AuditEngagementGroupMemberRepository memberRepository;
    @Autowired private AuditMasterDataItemRepository masterDataItemRepository;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = tenantRepository.findByCode("default").orElseThrow().getId();
        TenantContext.setTenantId(tenantId);
        TenantContext.setCurrentUser("dgcl-test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    /** So lieu mau cua chinh sheet PL 01F: B1=100, B2=98.6486, 1 loi dong 19 (tru 20%), tich du 3 muc diem cong. */
    @Test
    void quality_matchesSampleValuesOfSheetPl01F() {
        Map<String, LineValues> values = new HashMap<>();
        values.put("F013", new LineValues(false, false, false, false, 1, null));
        for (String key : List.of("F019", "F020", "F021")) {
            values.put(key, new LineValues(false, false, false, true, null, null));
        }
        QualityResult q = DgclScoring.quality(catalog.items(DgclAppendix.PL01F), values, 1.0, 0.9864864864864865);

        assertThat(q.weightedTotal()).isCloseTo(85.7297, within(0.001));
        assertThat(q.bonusPoints()).isCloseTo(10, within(0.0001));
        assertThat(q.grandTotal()).isCloseTo(95.7297, within(0.001));
        assertThat(q.teamClassification()).isEqualTo("Hoàn thành khá nhiệm vụ");
        assertThat(q.calcByKey().get("F004").points()).isCloseTo(80, within(0.0001));
    }

    /** test 29.9: diem PL01A >= 90 ma khong tich dong "gián đoạn..." thi = 100, tich thi giu nguyen; < 90 giu nguyen. */
    @Test
    void pl01a_disruptionRowRule() {
        List<DgclCriteriaCatalog.Item> items = catalog.items(DgclAppendix.PL01A);
        List<String> keys = items.stream().filter(DgclCriteriaCatalog.Item::isTick).map(DgclCriteriaCatalog.Item::key).limit(11).toList();
        String disruptionKey = items.stream().filter(i -> "DISRUPTION".equals(i.kind())).findFirst().orElseThrow().key();
        Map<String, LineValues> values = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            values.put(keys.get(i), new LineValues(true, i < 10, i >= 10, false, null, null)); // 10/11 = 90.9%
        }
        assertThat(DgclScoring.compliance(items, values).score()).isEqualTo(100d);
        values.put(disruptionKey, new LineValues(false, false, false, true, null, null));
        assertThat(DgclScoring.compliance(items, values).score()).isCloseTo(90.909, within(0.001));
        values.remove(disruptionKey);
        values.put(keys.get(9), new LineValues(true, false, true, false, null, null)); // 9/11 = 81.8%
        assertThat(DgclScoring.compliance(items, values).score()).isCloseTo(81.818, within(0.001));
    }

    /** test 29.9: phieu moi tu tich san NDTH + Tuân thủ theo cot "tick"; PL01B hien du moi mang nhung chi tich san CHUNG + mang
     * cua thanh vien; PL01F diem toi da I/II/III co dinh 100. */
    @Test
    void newSheets_arePreTickedBySegment() {
        EmployeeResponse lead = createEmployee("NV-DGCL-TL2");
        EmployeeResponse member = createEmployee("NV-DGCL-TV2");
        setDgcl(lead.id(), true, false);
        AuditEngagement engagement = createEngagement("CKT-DGCL-02", lead.id(), member.id());
        AuditMasterDataItem dp = new AuditMasterDataItem();
        dp.setTenantId(tenantId);
        dp.setCategory(AuditMasterDataCategory.BUSINESS_SEGMENT);
        dp.setCode("DP");
        dp.setName("Huy dong von");
        dp = masterDataItemRepository.save(dp);
        AuditEngagementGroupMember m = memberRepository.findAll().stream().filter(x -> x.getEmployeeId().equals(member.id())).findFirst().orElseThrow();
        m.setBusinessSegment1Id(dp.getId());
        memberRepository.save(m);
        CurrentUserPrincipal ev = principal(lead);
        UUID id = engagement.getId();

        Sheet a = service.getSheet(id, member.id().toString(), DgclAppendix.PL01A, ev);
        assertThat(a.saved()).isFalse();
        assertThat(a.summary().requiredCount()).isEqualTo(34);
        assertThat(a.summary().score()).isEqualTo(100d);

        Sheet b = service.getSheet(id, member.id().toString(), DgclAppendix.PL01B, ev);
        assertThat(b.lines()).hasSize(catalog.items(DgclAppendix.PL01B).size());
        assertThat(b.lines()).filteredOn(l -> l.required()).extracting(l -> l.segment()).containsOnly("CHUNG", "DP");
        assertThat(b.lines()).filteredOn(l -> l.tick() && "DP".equals(l.segment())).allMatch(l -> l.required() && l.compliant());
        assertThat(b.lines()).filteredOn(l -> l.tick() && "GA".equals(l.segment())).noneMatch(l -> l.required());

        Sheet leadB = service.getSheet(id, lead.id().toString(), DgclAppendix.PL01B, ev);
        assertThat(leadB.lines()).filteredOn(l -> l.required()).extracting(l -> l.segment()).containsOnly("CHUNG");

        service.saveSheet(id, member.id().toString(), DgclAppendix.PL01F, new SaveRequest(List.of(
                new LineInput("F002", false, false, false, false, null, java.math.BigDecimal.valueOf(50), null, null, null))), ev);
        Sheet f = service.getSheet(id, member.id().toString(), DgclAppendix.PL01F, ev);
        assertThat(f.lines().stream().filter(l -> l.key().equals("F002")).findFirst().orElseThrow().calcMax()).isEqualTo(100d);
    }

    @Test
    void fullFlow_evaluateConfirmControlAndExport() throws Exception {
        EmployeeResponse lead = createEmployee("NV-DGCL-TL");
        EmployeeResponse member = createEmployee("NV-DGCL-TV");
        EmployeeResponse evaluator = lead;
        EmployeeResponse controller = createEmployee("NV-DGCL-KS");
        EmployeeResponse outsider = createEmployee("NV-DGCL-OUT");
        EmployeeResponse outsideEvaluator = createEmployee("NV-DGCL-OEV");
        setDgcl(evaluator.id(), true, false);
        setDgcl(outsideEvaluator.id(), true, false);
        setDgcl(controller.id(), false, true);
        AuditEngagement engagement = createEngagement("CKT-DGCL-01", lead.id(), member.id());
        UUID id = engagement.getId();
        CurrentUserPrincipal ev = principal(evaluator);
        CurrentUserPrincipal ks = principal(controller);
        CurrentUserPrincipal out = principal(outsider);
        CurrentUserPrincipal oev = principal(outsideEvaluator);

        // test 29.9 muc 1: chi thanh vien doan (hoac NSD kiem soat DGCL) thay/vao duoc CKT
        assertThat(service.listEngagements(ev)).extracting(r -> r.id()).contains(id);
        assertThat(service.listEngagements(ks)).extracting(r -> r.id()).contains(id);
        assertThat(service.listEngagements(oev)).extracting(r -> r.id()).doesNotContain(id);
        assertThatThrownBy(() -> service.listSubjects(id, oev)).isInstanceOf(BusinessException.class);

        List<SubjectRow> subjects = service.listSubjects(id, ev);
        assertThat(subjects).extracting(SubjectRow::role).containsExactly("TD", "TV", null);
        assertThat(subjects.get(2).team()).isTrue();
        String memberKey = member.id().toString();

        SaveRequest pl01a = new SaveRequest(List.of(compliant("A002", true), compliant("A003", true), compliant("A004", true), compliant("A005", false)));
        assertThatThrownBy(() -> service.saveSheet(id, memberKey, DgclAppendix.PL01A, pl01a, out)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.saveSheet(id, memberKey, DgclAppendix.PL01A, pl01a, oev)).as("co quyen DGCL nhung khong thuoc doan")
                .isInstanceOf(BusinessException.class);

        Sheet a = service.saveSheet(id, memberKey, DgclAppendix.PL01A, pl01a, ev);
        assertThat(a.summary().score()).isCloseTo(75, within(0.0001));
        assertThat(a.lines().stream().filter(l -> l.key().equals("A005")).findFirst().orElseThrow().evaluatorName()).isEqualTo(evaluator.fullName());
        // test 30.9 muc 1: diem da Luu hien ngay ra man hinh ngoai, co pl01aConfirmed cho biet chua xac nhan
        SubjectRow saved = service.listSubjects(id, ev).get(1);
        assertThat(saved.pl01aScore().doubleValue()).isCloseTo(75, within(0.0001));
        assertThat(saved.pl01aConfirmed()).isFalse();
        service.confirm(id, memberKey, DgclAppendix.PL01A, ev);
        assertThat(service.listSubjects(id, ev).get(1).pl01aConfirmed()).isTrue();
        assertThatThrownBy(() -> service.saveSheet(id, memberKey, DgclAppendix.PL01A, pl01a, ev)).isInstanceOf(BusinessException.class);

        service.saveSheet(id, memberKey, DgclAppendix.PL01B, new SaveRequest(List.of(compliant("B002", true), compliant("B003", true))), ev);
        service.confirm(id, memberKey, DgclAppendix.PL01B, ev);

        assertThatThrownBy(() -> service.confirm(id, memberKey, DgclAppendix.PL01F, ev)).isInstanceOf(BusinessException.class);
        Sheet f = service.saveSheet(id, memberKey, DgclAppendix.PL01F, new SaveRequest(List.of(
                new LineInput("F013", false, false, false, false, 1, null, "1 loi", null, null),
                new LineInput("F019", false, false, false, true, null, null, null, null, null))), ev);
        // 75*10% + 100*20% + (100-20)*70% = 83.5; + diem cong 2% x 100 = 85.5
        assertThat(f.summary().score()).isCloseTo(85.5, within(0.0001));
        assertThat(f.summary().classification()).isEqualTo("Chất lượng khá");
        service.confirm(id, memberKey, DgclAppendix.PL01F, ev);

        SubjectRow row = service.listSubjects(id, ev).get(1);
        assertThat(row.pl01fScore().doubleValue()).isCloseTo(85.5, within(0.0001));
        assertThat(row.classification()).isEqualTo("Chất lượng khá");
        assertThat(row.confirmedCount()).isEqualTo(3);
        assertThatThrownBy(() -> service.unconfirm(id, memberKey, DgclAppendix.PL01A, ev))
                .as("PL01F da xac nhan dang dung diem PL01A").isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> service.control(id, memberKey, DgclAppendix.PL01F, ev)).isInstanceOf(BusinessException.class);
        Sheet controlled = service.control(id, memberKey, DgclAppendix.PL01F, ks);
        assertThat(controlled.controlledBy()).isEqualTo(controller.fullName());
        assertThatThrownBy(() -> service.unconfirm(id, memberKey, DgclAppendix.PL01F, ev)).isInstanceOf(BusinessException.class);
        assertThat(service.getSheet(id, memberKey, DgclAppendix.PL01F, ev).canUnconfirm()).isFalse();
        service.uncontrol(id, memberKey, DgclAppendix.PL01F, ks);
        assertThat(service.unconfirm(id, memberKey, DgclAppendix.PL01F, ev).confirmed()).isFalse();
        SubjectRow unconfirmed = service.listSubjects(id, ev).get(1);
        assertThat(unconfirmed.pl01fScore().doubleValue()).isCloseTo(85.5, within(0.0001));
        assertThat(unconfirmed.pl01fConfirmed()).isFalse();
        assertThat(texts(service.exportPl04b1(id, ev))).as("PL04B1 chi dung PL01F da xac nhan").doesNotContain("Chất lượng khá");

        service.confirm(id, memberKey, DgclAppendix.PL01F, ev);
        assertThat(texts(service.exportPl04b1(id, ev)))
                .contains("Chất lượng khá", "Trưởng đoàn: " + lead.fullName(), "Thành viên 1: " + member.fullName(), "Đoàn kiểm toán");

        // test 30.9 muc 2-4: xuat phieu vao mau FORM_PL01A/B/F
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(service.exportSheet(id, memberKey, DgclAppendix.PL01A, ev)))) {
            var sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(5).getCell(0).getStringCellValue()).contains("Thành viên: " + member.fullName()).doesNotContain("[");
            assertThat(sheet.getRow(16).getCell(1).getStringCellValue()).startsWith("Xác định mục tiêu"); // A002
            assertThat(sheet.getRow(16).getCell(2).getStringCellValue()).isEqualTo("X");
            assertThat(sheet.getRow(16).getCell(3).getStringCellValue()).isEqualTo("X");
            assertThat(sheet.getRow(19).getCell(4).getStringCellValue()).isEqualTo("X"); // A005 khong tuan thu
            assertThat(sheet.getRow(55).getCell(2).getNumericCellValue()).isEqualTo(4); // IV
            assertThat(sheet.getRow(56).getCell(3).getNumericCellValue()).isCloseTo(0.75, within(0.0001)); // V
            assertThat(sheet.getRow(58).getCell(3).getNumericCellValue()).isCloseTo(75, within(0.0001)); // VI
        }
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(service.exportSheet(id, memberKey, DgclAppendix.PL01B, ev)))) {
            var sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(15).getCell(2).getStringCellValue()).isEqualTo("X"); // B002
            assertThat(sheet.getRow(416).getCell(2).getNumericCellValue()).isEqualTo(2); // IV
            assertThat(sheet.getRow(418).getCell(3).getNumericCellValue()).isCloseTo(100, within(0.0001)); // VI
        }
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(service.exportSheet(id, memberKey, DgclAppendix.PL01F, ev)))) {
            var sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(26).getCell(1).getStringCellValue()).startsWith("Phát hiện ra sai phạm trọng yếu"); // F013, mau khong co F005
            assertThat(sheet.getRow(26).getCell(3).getNumericCellValue()).isEqualTo(1);
            assertThat(sheet.getRow(26).getCell(7).getStringCellValue()).isEqualTo("1 loi");
            assertThat(sheet.getRow(32).getCell(3).getStringCellValue()).isEqualTo("X"); // F019 diem cong muc 1
            assertThat(sheet.getRow(39).getCell(5).getNumericCellValue()).isCloseTo(85.5, within(0.0001)); // VI
            assertThat(sheet.getRow(40).getCell(2).getStringCellValue()).isEqualTo("Chất lượng khá");
        }
    }

    private List<String> texts(byte[] excel) throws Exception {
        List<String> texts = new ArrayList<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            for (Row r : wb.getSheetAt(0)) {
                for (Cell c : r) {
                    if (c.getCellType() == CellType.STRING) {
                        texts.add(c.getStringCellValue());
                    }
                }
            }
        }
        return texts;
    }

    private LineInput compliant(String key, boolean ok) {
        return new LineInput(key, true, ok, !ok, false, null, null, null, null, null);
    }

    private CurrentUserPrincipal principal(EmployeeResponse employee) {
        return new CurrentUserPrincipal(UUID.randomUUID(), "u-" + employee.employeeCode(), tenantId, employee.employeeCode(), List.of(), List.of(),
                UUID.randomUUID().toString());
    }

    private void setDgcl(UUID employeeId, boolean evaluate, boolean control) {
        capabilityService.bulkUpdate(List.of(new AuditEmployeeCapabilityItemRequest(employeeId,
                false, false, false, false, false, false, false, false, false, false, false, false, false, evaluate, control)));
    }

    private AuditEngagement createEngagement(String code, UUID leadId, UUID memberId) {
        AuditObjectUnit unit = new AuditObjectUnit();
        unit.setTenantId(tenantId);
        unit.setCode("DGCL01");
        unit.setName("Chi nhanh DGCL");
        unit.setUnitType("CN");
        unit = unitRepository.save(unit);

        AuditEngagement engagement = new AuditEngagement();
        engagement.setTenantId(tenantId);
        engagement.setCode(code);
        engagement.setAuditObjectUnitId(unit.getId());
        engagement.setYear(2026);
        engagement.setExpectedMonth(9);
        engagement.setDecisionDate(LocalDate.now());
        engagement.setTeamLeadEmployeeId(leadId);
        engagement.setDecisionNumber("QD-" + code);
        engagement = engagementRepository.save(engagement);

        AuditEngagementGroup group = new AuditEngagementGroup();
        group.setTenantId(tenantId);
        group.setAuditEngagementId(engagement.getId());
        group.setGroupCode("N01");
        group.setLeaderEmployeeId(leadId);
        group = groupRepository.save(group);

        AuditEngagementGroupMember m = new AuditEngagementGroupMember();
        m.setTenantId(tenantId);
        m.setGroupId(group.getId());
        m.setEmployeeId(memberId);
        memberRepository.save(m);
        return engagement;
    }

    private EmployeeResponse createEmployee(String code) {
        return employeeService.create(new EmployeeRequest(code, "Nguyen Van " + code, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null, null));
    }
}
