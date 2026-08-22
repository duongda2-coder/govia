package com.govia.identity;

import com.govia.identity.dto.OrgUnitActiveRequest;
import com.govia.identity.dto.OrganizationUnitRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kich ban test cho cay to chuc (Khoi/Trung tam/Phong ban/Bo phan):
 * tao cay 4 cap, trung ma, level_code khong hop le, vong lap cha-con, bat/tat don vi, chan truy cap khong token.
 */
class OrganizationUnitApiTest extends AbstractApiTest {

    @Test
    void createFourLevelHierarchyAndFetchTree() throws Exception {
        String khoiId = createUnit("KH-01", "Khoi Cong nghe", "001", null);
        String ttId = createUnit("TT-01", "Trung tam Phat trien", "002", khoiId);
        String pbId = createUnit("PB-01", "Phong Backend", "003", ttId);
        createUnit("BP-01", "Bo phan Java", "004", pbId);

        mockMvc.perform(get("/api/org-units/tree").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code=='KH-01')]", hasSize(1)))
                .andExpect(jsonPath("$.data[?(@.code=='KH-01')].children[*].code", hasItem("TT-01")))
                .andExpect(jsonPath(
                        "$.data[?(@.code=='KH-01')].children[?(@.code=='TT-01')].children[*].code",
                        hasItem("PB-01")))
                .andExpect(jsonPath(
                        "$.data[?(@.code=='KH-01')].children[?(@.code=='TT-01')].children[?(@.code=='PB-01')].children[*].code",
                        hasItem("BP-01")));
    }

    @Test
    void duplicateCodeInSameTenantIsRejected() throws Exception {
        createUnit("DUP-01", "Don vi 1", "001", null);

        OrganizationUnitRequest dup = new OrganizationUnitRequest("DUP-01", "Don vi 2", null, "001", null, null);
        mockMvc.perform(post("/api/org-units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ORG_UNIT_CODE_DUPLICATE"));
    }

    @Test
    void invalidLevelCodeIsRejected() throws Exception {
        OrganizationUnitRequest invalid = new OrganizationUnitRequest("INV-01", "Don vi invalid", null, "999", null, null);
        mockMvc.perform(post("/api/org-units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ORG_UNIT_INVALID_LEVEL_CODE"));
    }

    @Test
    void circularParentAssignmentIsRejected() throws Exception {
        String parentId = createUnit("CIR-P", "Cha", "001", null);
        String childId = createUnit("CIR-C", "Con", "002", parentId);

        // Thu doi cha cua "Cha" thanh chinh "Con" cua no -> phai bi tu choi vi tao vong lap
        OrganizationUnitRequest update = new OrganizationUnitRequest("CIR-P", "Cha", null, "001",
                UUID.fromString(childId), null);
        mockMvc.perform(put("/api/org-units/" + parentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ORG_UNIT_CIRCULAR"));
    }

    @Test
    void deactivateThenReactivateUnit() throws Exception {
        String id = createUnit("ACT-01", "Don vi active test", "001", null);

        mockMvc.perform(patch("/api/org-units/" + id + "/active")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrgUnitActiveRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(patch("/api/org-units/" + id + "/active")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrgUnitActiveRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/org-units"))
                .andExpect(status().is4xxClientError());
    }

    private String createUnit(String code, String name, String levelCode, String parentId) throws Exception {
        OrganizationUnitRequest request = new OrganizationUnitRequest(code, name, null, levelCode,
                parentId == null ? null : UUID.fromString(parentId), null);
        String body = mockMvc.perform(post("/api/org-units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data").get("id").asText();
    }
}
