package com.govia.identity;

import com.govia.audit.appendix.dto.AuditAppendixRequest;
import com.govia.audit.appendix.dto.AuditAppendixResponse;
import com.govia.audit.appendix.service.AuditAppendixService;
import com.govia.audit.masterdata.dto.MasterDataItemRequest;
import com.govia.audit.masterdata.entity.AuditMasterDataCategory;
import com.govia.audit.masterdata.service.MasterDataItemService;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Tenant;
import com.govia.identity.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Kich ban test CRUD cho danh muc "Quan ly phu luc" (sheet ZTC_phuluc). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditAppendixServiceTest {

    @Autowired
    private AuditAppendixService appendixService;

    @Autowired
    private MasterDataItemService masterDataItemService;

    @Autowired
    private TenantRepository tenantRepository;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.findByCode("default").orElseThrow();
        TenantContext.setTenantId(tenant.getId());
        TenantContext.setCurrentUser("test-user");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private AuditAppendixRequest sampleRequest(java.util.UUID segmentId, String appendixCode) {
        return new AuditAppendixRequest(segmentId, "ZTC_CM_NTD10", appendixCode, "07B/BKS-KTNB", true);
    }

    @Test
    void create_succeedsWithBusinessSegment() {
        var segment = masterDataItemService.create(AuditMasterDataCategory.BUSINESS_SEGMENT,
                new MasterDataItemRequest("CD", "Chung", null, null, null, null, null, true));

        AuditAppendixResponse created = appendixService.create(sampleRequest(segment.id(), "07B/BKS-KTNB-T01"));

        assertThat(created.appendixCode()).isEqualTo("07B/BKS-KTNB-T01");
        assertThat(created.businessSegmentCode()).isEqualTo("CD");
        assertThat(created.active()).isTrue();
    }

    @Test
    void duplicateAppendixCode_isRejected() {
        appendixService.create(sampleRequest(null, "07B/BKS-KTNB-T02"));

        assertThatThrownBy(() -> appendixService.create(sampleRequest(null, "07B/BKS-KTNB-T02")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("da ton tai");
    }

    @Test
    void update_changesFields() {
        AuditAppendixResponse created = appendixService.create(sampleRequest(null, "07B/BKS-KTNB-T03"));

        AuditAppendixResponse updated = appendixService.update(created.id(),
                new AuditAppendixRequest(null, "ZTC_CM_NTD11", "07B/BKS-KTNB-T03", "Ghi chu moi", false));

        assertThat(updated.sampleType()).isEqualTo("ZTC_CM_NTD11");
        assertThat(updated.note()).isEqualTo("Ghi chu moi");
        assertThat(updated.active()).isFalse();
    }

    @Test
    void delete_removesItem() {
        AuditAppendixResponse created = appendixService.create(sampleRequest(null, "07B/BKS-KTNB-T04"));

        appendixService.delete(created.id());

        assertThat(appendixService.list()).extracting(AuditAppendixResponse::id).doesNotContain(created.id());
    }

    @Test
    void exportExcelAndWord_produceNonEmptyFiles() {
        appendixService.create(sampleRequest(null, "07B/BKS-KTNB-T05"));

        byte[] excel = appendixService.exportExcel();
        byte[] word = appendixService.exportWord();

        assertThat(excel).isNotEmpty();
        assertThat(word).isNotEmpty();
    }
}
