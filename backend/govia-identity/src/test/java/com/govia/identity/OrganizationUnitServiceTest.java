package com.govia.identity;

import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import com.govia.identity.dto.OrgUnitTreeNode;
import com.govia.identity.dto.OrganizationUnitRequest;
import com.govia.identity.dto.OrganizationUnitResponse;
import com.govia.identity.entity.Tenant;
import com.govia.identity.repository.TenantRepository;
import com.govia.identity.service.OrganizationUnitService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kich ban test cho cay to chuc (Khoi/Trung tam/Phong ban/Bo phan qua parent_id + level_code).
 * Dung H2 in-memory (profile "test", xem application-test.yml) - khong can Postgres that.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrganizationUnitServiceTest {

    @Autowired
    private OrganizationUnitService organizationUnitService;

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
    void createRootAndChildUnit_buildsCorrectTree() {
        OrganizationUnitResponse khoi = organizationUnitService.create(
                new OrganizationUnitRequest("KHOI-CNTT", "Khoi Cong nghe thong tin", "DIVISION", "001", null, null));
        OrganizationUnitResponse phong = organizationUnitService.create(
                new OrganizationUnitRequest("PHONG-DEV", "Phong Phat trien", "DEPARTMENT", "003", khoi.id(), null));

        List<OrgUnitTreeNode> tree = organizationUnitService.tree();

        OrgUnitTreeNode khoiNode = tree.stream().filter(n -> n.id().equals(khoi.id())).findFirst().orElseThrow();
        assertThat(khoiNode.children()).extracting(OrgUnitTreeNode::id).contains(phong.id());
    }

    @Test
    void duplicateCode_isRejected() {
        organizationUnitService.create(new OrganizationUnitRequest("DUP-01", "Don vi 1", null, null, null, null));

        assertThatThrownBy(() -> organizationUnitService.create(
                new OrganizationUnitRequest("DUP-01", "Don vi 2", null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("da ton tai");
    }

    @Test
    void invalidLevelCode_isRejected() {
        assertThatThrownBy(() -> organizationUnitService.create(
                new OrganizationUnitRequest("INV-01", "Don vi loi", null, "999", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("level_code");
    }

    @Test
    void parentNotFound_isRejected() {
        assertThatThrownBy(() -> organizationUnitService.create(
                new OrganizationUnitRequest("ORPHAN-01", "Don vi mo coi", null, null, UUID.randomUUID(), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Don vi cha");
    }

    @Test
    void circularParent_isRejected() {
        OrganizationUnitResponse parent = organizationUnitService.create(
                new OrganizationUnitRequest("PAR-01", "Cha", null, "001", null, null));
        OrganizationUnitResponse child = organizationUnitService.create(
                new OrganizationUnitRequest("CHI-01", "Con", null, "002", parent.id(), null));

        // Thu bien "Cha" thanh con cua chinh con cua no -> vong lap
        assertThatThrownBy(() -> organizationUnitService.update(parent.id(),
                new OrganizationUnitRequest("PAR-01", "Cha", null, "001", child.id(), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vong lap");
    }

    @Test
    void setActive_togglesStatus() {
        OrganizationUnitResponse unit = organizationUnitService.create(
                new OrganizationUnitRequest("ACT-01", "Don vi test active", null, null, null, null));
        assertThat(unit.active()).isTrue();

        OrganizationUnitResponse deactivated = organizationUnitService.setActive(unit.id(), false);
        assertThat(deactivated.active()).isFalse();

        OrganizationUnitResponse reactivated = organizationUnitService.setActive(unit.id(), true);
        assertThat(reactivated.active()).isTrue();
    }
}
