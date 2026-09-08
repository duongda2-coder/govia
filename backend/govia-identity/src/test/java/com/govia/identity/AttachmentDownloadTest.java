package com.govia.identity;

import com.govia.core.attachment.Attachment;
import com.govia.core.attachment.AttachmentService;
import com.govia.core.attachment.AttachmentStorageProperties;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.entity.Tenant;
import com.govia.identity.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiem chung fix bug "Báo cáo tiến độ -> tải file bị thất bại" (xem
 * LocalFileAttachmentServiceImpl.loadAsResource): getRootPath() phai luon tra ve duong dan TUYET
 * DOI (khong phu thuoc working directory tai thoi diem goi), va loadAsResource() phai bao loi 404
 * SACH SE ngay tu dau khi file vat ly khong con tren dia, thay vi de Spring phat hien luc dang
 * stream response (qua muon, tra ve response bi vo cho client).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AttachmentDownloadTest {

    @Autowired
    private AttachmentService attachmentService;
    @Autowired
    private AttachmentStorageProperties attachmentStorageProperties;
    @Autowired
    private TenantRepository tenantRepository;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.findByCode("default").orElseThrow();
        tenantId = tenant.getId();
        TenantContext.setTenantId(tenantId);
        TenantContext.setCurrentUser("test-user");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getRootPath_alwaysReturnsAbsolutePath() {
        assertThat(Path.of(attachmentStorageProperties.getRootPath()).isAbsolute()).isTrue();
    }

    @Test
    void uploadThenDownload_roundTripsSuccessfully() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "bao_cao.txt", "text/plain", "noi dung bao cao".getBytes(StandardCharsets.UTF_8));
        Attachment attachment = attachmentService.upload("AUDIT_PROGRESS_REPORT", UUID.randomUUID(), file);

        assertThat(Path.of(attachment.getStoragePath())).isAbsolute();

        Resource resource = attachmentService.loadAsResource(attachment.getId());
        assertThat(resource.exists()).isTrue();
        assertThat(new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("noi dung bao cao");
    }

    /** Mo phong dung bug da bao cao: dong attachment van con trong DB (list/count van chay binh
     * thuong) nhung file vat ly da khong con o duong dan luu (vd doi working directory luc khoi
     * dong app) - phai bao 404 ro rang thay vi de vo response luc dang stream. */
    @Test
    void loadAsResource_missingPhysicalFile_throwsCleanNotFoundInsteadOfBreakingMidStream() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "bao_cao_2.txt", "text/plain", "du lieu".getBytes(StandardCharsets.UTF_8));
        Attachment attachment = attachmentService.upload("AUDIT_PROGRESS_REPORT", UUID.randomUUID(), file);

        Files.delete(Path.of(attachment.getStoragePath()));

        assertThatThrownBy(() -> attachmentService.loadAsResource(attachment.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
