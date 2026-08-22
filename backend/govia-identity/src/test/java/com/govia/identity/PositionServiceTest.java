package com.govia.identity;

import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.PositionRequest;
import com.govia.identity.dto.PositionResponse;
import com.govia.identity.entity.Tenant;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.service.PositionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Kich ban test CRUD chuc danh (master-data): tao, trung ma, bat/tat active. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PositionServiceTest {

    @Autowired
    private PositionService positionService;

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

    @Test
    void createPosition_succeedsActiveByDefault() {
        PositionResponse created = positionService.create(new PositionRequest("POS-T01", "Ky su phan mem"));

        assertThat(created.active()).isTrue();
        assertThat(created.name()).isEqualTo("Ky su phan mem");
    }

    @Test
    void duplicateCode_isRejected() {
        positionService.create(new PositionRequest("POS-T02", "Vi tri 1"));

        assertThatThrownBy(() -> positionService.create(new PositionRequest("POS-T02", "Vi tri 2")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("da ton tai");
    }

    @Test
    void setActive_togglesStatus() {
        PositionResponse position = positionService.create(new PositionRequest("POS-T03", "Vi tri test"));

        PositionResponse deactivated = positionService.setActive(position.id(), false);
        assertThat(deactivated.active()).isFalse();

        PositionResponse reactivated = positionService.setActive(position.id(), true);
        assertThat(reactivated.active()).isTrue();
    }
}
