package com.govia.identity;

import com.govia.audit.masterdata.dto.MasterDataItemRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiem chung co che danh muc DUNG CHUNG cua module Kiem toan noi bo (xem MasterDataItemService):
 * 1 bang/1 API cho toan bo danh muc con lai (sau khi bo Kiem toan/Phat hien/Kiem soat/Quy trinh/
 * Tuan thu), phan biet qua {category} tren URL.
 */
class AuditMasterDataApiTest extends AbstractApiTest {

    @Test
    void listCategoriesReturnsAllConfiguredTypes() throws Exception {
        mockMvc.perform(get("/api/audit/master-data/categories").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(greaterThanOrEqualTo(10))))
                .andExpect(jsonPath("$.data[?(@.code=='RISK_LEVEL')].group", org.hamcrest.Matchers.hasItem("RISK")));
    }

    @Test
    void createUpdateDeleteItemInRiskLevelCategory() throws Exception {
        String createBody = mockMvc.perform(post("/api/audit/master-data/RISK_LEVEL")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MasterDataItemRequest("HIGH", "Cao", "Muc do rui ro cao", null, null, null, 1, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("RISK_LEVEL"))
                .andExpect(jsonPath("$.data.code").value("HIGH"))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createBody).get("data").get("id").asText();

        mockMvc.perform(get("/api/audit/master-data/RISK_LEVEL").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code=='HIGH')].name", org.hamcrest.Matchers.hasItem("Cao")));

        mockMvc.perform(put("/api/audit/master-data/RISK_LEVEL/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MasterDataItemRequest("HIGH", "Cao (sua)", null, null, null, null, 1, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Cao (sua)"));

        mockMvc.perform(delete("/api/audit/master-data/RISK_LEVEL/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/audit/master-data/RISK_LEVEL").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code=='HIGH')]", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void duplicateCodeInSameCategoryIsRejected() throws Exception {
        mockMvc.perform(post("/api/audit/master-data/RISK_LEVEL")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MasterDataItemRequest("DUP", "Trung ma", null, null, null, null, null, true))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/audit/master-data/RISK_LEVEL")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MasterDataItemRequest("DUP", "Trung ma 2", null, null, null, null, null, true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MASTER_DATA_CODE_DUPLICATE"));
    }

    @Test
    void sameCodeAllowedAcrossDifferentCategories() throws Exception {
        // Dam bao khac category thi khong dung chung 1 rang buoc unique code - dung 2 category rieng
        // biet, khac voi cac ma da dung o cac test khac (rollback rieng transaction moi test).
        mockMvc.perform(post("/api/audit/master-data/RISK_LEVEL")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MasterDataItemRequest("SAME-CODE", "Muc do rui ro", null, null, null, null, null, true))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/audit/master-data/CURRENCY")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MasterDataItemRequest("SAME-CODE", "Tien te khac", null, null, null, null, null, true))))
                .andExpect(status().isOk());
    }

    @Test
    void parentChildRelationshipIsSupportedGenerically() throws Exception {
        // parentId la co che chung tren AuditMasterDataItem (khong rieng 1 category nao) - dung
        // CURRENCY de kiem chung co che nay van hoat dong dung sau khi bo cac danh muc co phan cap
        // rieng (BUSINESS_PROCESS/BUSINESS_PROCESS_STEP da bi go bo).
        String parentBody = mockMvc.perform(post("/api/audit/master-data/CURRENCY")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MasterDataItemRequest("PARENT-CCY", "Nhom tien te", null, null, null, null, null, true))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String parentId = objectMapper.readTree(parentBody).get("data").get("id").asText();

        mockMvc.perform(post("/api/audit/master-data/CURRENCY")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MasterDataItemRequest("CHILD-CCY", "Tien te con", null,
                                        java.util.UUID.fromString(parentId), null, null, null, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value(parentId));
    }

    @Test
    void importExportExcelRoundTrip() throws Exception {
        String createBody = mockMvc.perform(post("/api/audit/master-data/CURRENCY")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MasterDataItemRequest("VND", "Viet Nam Dong", null, null, null, null, null, true))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createBody).get("data").get("id").asText();

        byte[] excel = mockMvc.perform(get("/api/audit/master-data/CURRENCY/export/excel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        // Xoa di truoc khi import lai - vi import goi create(), ma code "VND" da ton tai se bao trung.
        mockMvc.perform(delete("/api/audit/master-data/CURRENCY/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        MockMultipartFile file = new MockMultipartFile("file", "currency.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excel);

        mockMvc.perform(multipart("/api/audit/master-data/CURRENCY/import")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failureCount").value(0));
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/audit/master-data/RISK_LEVEL"))
                .andExpect(status().is4xxClientError());
    }
}
