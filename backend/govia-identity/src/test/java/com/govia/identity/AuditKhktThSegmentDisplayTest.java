package com.govia.identity;

import com.govia.audit.khkt.bp.entity.AuditKhktBpConfirmed;
import com.govia.audit.khkt.bp.entity.AuditKhktBpConfirmedSegment;
import com.govia.audit.khkt.bp.repository.AuditKhktBpConfirmedRepository;
import com.govia.audit.khkt.bp.repository.AuditKhktBpConfirmedSegmentRepository;
import com.govia.audit.khkt.common.entity.AuditKhktSourceType;
import com.govia.audit.khkt.th.dto.AuditKhktThCandidateUpdateRequest;
import com.govia.audit.khkt.th.dto.AuditKhktThRowResponse;
import com.govia.audit.khkt.th.service.AuditKhktThService;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.entity.AuditMasterDataItem;
import com.govia.audit.masterdata.repository.AuditMasterDataItemRepository;
import com.govia.core.tenant.TenantContext;
import com.govia.identity.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Man "Danh sach DTKT nam cua Phong ke hoach" (TH): cot "BP de xuat LVKT: <linh vuc>" hien danh sach
 * PHONG da tich linh vuc do o BP2 (vd "PGS,PKH,KTNB1"), cot "Linh vuc kiem toan" la cac linh vuc TH da tich
 * (vd "AM,CE") - chay tren H2 in-memory. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditKhktThSegmentDisplayTest {

    private static final int YEAR = 2098;

    @Autowired private AuditKhktThService thService;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private AuditMasterDataItemRepository masterDataItemRepository;
    @Autowired private AuditKhktBpConfirmedRepository bpConfirmedRepository;
    @Autowired private AuditKhktBpConfirmedSegmentRepository bpConfirmedSegmentRepository;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = tenantRepository.findByCode("default").orElseThrow().getId();
        TenantContext.setTenantId(tenantId);
        TenantContext.setCurrentUser("test-user");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void bpSegmentColumnListsProposingDepartmentsInCatalogOrder() {
        UUID it = item(AuditMasterDataCategory.BUSINESS_SEGMENT, "T-IT", 900);
        UUID am = item(AuditMasterDataCategory.BUSINESS_SEGMENT, "T-AM", 901);
        // tao PGS/PKH/KTNB1 theo thu tu danh muc; luu vao BP2 theo thu tu khac de chung minh khong phu thuoc thu tu luu
        UUID pgs = item(AuditMasterDataCategory.DEPARTMENT, "T-PGS", 900);
        UUID pkh = item(AuditMasterDataCategory.DEPARTMENT, "T-PKH", 901);
        UUID ktnb1 = item(AuditMasterDataCategory.DEPARTMENT, "T-KTNB1", 902);

        bpRow(ktnb1, "OBJ-1", it);
        bpRow(pgs, "OBJ-1", it, am);
        bpRow(pkh, "OBJ-1", it);

        thService.sync(YEAR);
        AuditKhktThRowResponse row = thService.list(YEAR).get(0);

        assertThat(row.bpProposedSegmentDepartments().get("T-IT")).containsExactly("T-PGS", "T-PKH", "T-KTNB1");
        assertThat(row.bpProposedSegmentDepartments().get("T-AM")).containsExactly("T-PGS");
        assertThat(row.bpProposedSegmentDepartments()).doesNotContainKey("T-CE");
    }

    @Test
    void auditScopeIsTheTickedThSegmentsJoinedByComma() {
        UUID am = item(AuditMasterDataCategory.BUSINESS_SEGMENT, "T-AM", 901);
        UUID ce = item(AuditMasterDataCategory.BUSINESS_SEGMENT, "T-CE", 902);
        UUID dept = item(AuditMasterDataCategory.DEPARTMENT, "T-PGS", 900);
        bpRow(dept, "OBJ-1", am);
        thService.sync(YEAR);
        AuditKhktThRowResponse row = thService.list(YEAR).get(0);
        assertThat(row.auditScope()).isNull();

        // tich CE truoc AM nhung ket qua van theo thu tu danh muc: AM,CE
        AuditKhktThRowResponse updated = thService.update(row.id(), new AuditKhktThCandidateUpdateRequest(
                null, null, false, false, false, "nhap tay bi bo qua", null, null, null, null, List.of(ce, am)));

        assertThat(updated.auditScope()).isEqualTo("T-AM,T-CE");
        assertThat(updated.thBusinessSegmentCodes()).containsExactly("T-AM", "T-CE");

        AuditKhktThRowResponse cleared = thService.update(row.id(), new AuditKhktThCandidateUpdateRequest(
                null, null, false, false, false, null, null, null, null, null, List.of()));
        assertThat(cleared.auditScope()).isNull();
    }

    private UUID item(AuditMasterDataCategory category, String code, int sortOrder) {
        AuditMasterDataItem item = new AuditMasterDataItem();
        item.setTenantId(tenantId);
        item.setCategory(category);
        item.setCode(code);
        item.setName(code);
        item.setSortOrder(sortOrder);
        return masterDataItemRepository.save(item).getId();
    }

    private void bpRow(UUID departmentId, String objectCode, UUID... segmentIds) {
        AuditKhktBpConfirmed row = new AuditKhktBpConfirmed();
        row.setTenantId(tenantId);
        row.setDepartmentId(departmentId);
        row.setYear(YEAR);
        row.setSourceType(AuditKhktSourceType.OTHER);
        row.setAuditObjectCode(objectCode);
        row.setAuditObjectName(objectCode);
        row = bpConfirmedRepository.save(row);
        for (UUID segmentId : segmentIds) {
            AuditKhktBpConfirmedSegment link = new AuditKhktBpConfirmedSegment();
            link.setTenantId(tenantId);
            link.setConfirmedId(row.getId());
            link.setBusinessSegmentId(segmentId);
            bpConfirmedSegmentRepository.save(link);
        }
    }
}
