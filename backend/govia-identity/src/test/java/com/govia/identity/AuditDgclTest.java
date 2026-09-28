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

    @Test
    void fullFlow_evaluateConfirmControlAndExport() throws Exception {
        EmployeeResponse lead = createEmployee("NV-DGCL-TL");
        EmployeeResponse member = createEmployee("NV-DGCL-TV");
        EmployeeResponse evaluator = createEmployee("NV-DGCL-EV");
        EmployeeResponse controller = createEmployee("NV-DGCL-KS");
        EmployeeResponse outsider = createEmployee("NV-DGCL-OUT");
        setDgcl(evaluator.id(), true, false);
        setDgcl(controller.id(), false, true);
        AuditEngagement engagement = createEngagement("CKT-DGCL-01", lead.id(), member.id());
        UUID id = engagement.getId();
        CurrentUserPrincipal ev = principal(evaluator);
        CurrentUserPrincipal ks = principal(controller);
        CurrentUserPrincipal out = principal(outsider);

        List<SubjectRow> subjects = service.listSubjects(id);
        assertThat(subjects).extracting(SubjectRow::role).containsExactly("TD", "TV", null);
        assertThat(subjects.get(2).team()).isTrue();
        String memberKey = member.id().toString();

        SaveRequest pl01a = new SaveRequest(List.of(compliant("A002", true), compliant("A003", true), compliant("A004", true), compliant("A005", false)));
        assertThatThrownBy(() -> service.saveSheet(id, memberKey, DgclAppendix.PL01A, pl01a, out)).isInstanceOf(BusinessException.class);

        Sheet a = service.saveSheet(id, memberKey, DgclAppendix.PL01A, pl01a, ev);
        assertThat(a.summary().score()).isCloseTo(75, within(0.0001));
        assertThat(a.lines().stream().filter(l -> l.key().equals("A005")).findFirst().orElseThrow().evaluatorName()).isEqualTo(evaluator.fullName());
        assertThat(service.listSubjects(id).get(1).pl01aScore()).as("chua xac nhan thi chua day diem ra ngoai").isNull();
        service.confirm(id, memberKey, DgclAppendix.PL01A, ev);
        assertThat(service.listSubjects(id).get(1).pl01aScore().doubleValue()).isCloseTo(75, within(0.0001));
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

        SubjectRow row = service.listSubjects(id).get(1);
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
        assertThat(service.listSubjects(id).get(1).pl01fScore()).isNull();

        service.confirm(id, memberKey, DgclAppendix.PL01F, ev);
        byte[] excel = service.exportPl04b1(id);
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
        assertThat(texts).contains("Chất lượng khá", "Trưởng đoàn: " + lead.fullName(), "Thành viên 1: " + member.fullName(), "Đoàn kiểm toán");
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
