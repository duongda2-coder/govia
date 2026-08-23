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
 * 1 bang/1 API cho toan bo 23 loai danh muc, phan biet qua {category} tren URL.
 */
class AuditMasterDataApiTest extends AbstractApiTest {

    @Test
    void listCategoriesReturnsAllConfiguredTypes() throws Exception {
        mockMvc.perform(get("/api/audit/master-data/categories").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(greaterThanOrEqualTo(20))))
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
        mockMvc.perform(post("/api/audit/master-data/PRIORITY")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MasterDataItemRequest("HIGH", "Cao", null, null, null, null, null, true))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/audit/master-data/PRIORITY")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MasterDataItemRequest("HIGH", "Cao 2", null, null, null, null, null, true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MASTER_DATA_CODE_DUPLICATE"));
    }

    @Test
    void sameCodeAllowedAcrossDifferentCategories() throws Exception {
        // Ma "HIGH" da dung cho PRIORITY o test khac (rollback rieng transaction) - dam bao khac
        // category thi khong dung code check chung, dung 1 category rieng chua dung ("REGULATION").
        mockMvc.perform(post("/api/audit/master-data/PRIORITY")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MasterDataItemRequest("SAME-CODE", "Uu tien cao", null, null, null, null, null, true))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/audit/master-data/REGULATION")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MasterDataItemRequest("SAME-CODE", "Luat khac", null, null, null, null, null, true))))
                .andExpect(status().isOk());
    }

    @Test
    void parentChildRelationshipForBusinessProcessStep() throws Exception {
        String parentBody = mockMvc.perform(post("/api/audit/master-data/BUSINESS_PROCESS")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MasterDataItemRequest("PROC-01", "Quy trinh mua hang", null, null, null, null, null, true))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String parentId = objectMapper.readTree(parentBody).get("data").get("id").asText();

        mockMvc.perform(post("/api/audit/master-data/BUSINESS_PROCESS_STEP")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MasterDataItemRequest("STEP-01", "De nghi mua", null,
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
