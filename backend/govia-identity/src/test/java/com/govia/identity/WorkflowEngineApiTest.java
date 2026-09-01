package com.govia.identity;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiem chung ha tang Workflow (Flowable engine nhung trong govia-identity) chay dung end-to-end:
 * dinh nghia mau "simple_approval" da duoc WorkflowSampleProcessSeeder trien khai san luc boot cho
 * tenant "default", start 1 instance, hoan tat lan luot task Submit roi Approve, xac nhan quy trinh
 * ket thuc dung nhanh (Approved) qua HistoryService.
 */
class WorkflowEngineApiTest extends AbstractApiTest {

    @Test
    void sampleProcessDefinitionIsAutoDeployedForDefaultTenant() throws Exception {
        mockMvc.perform(get("/api/workflow/process-definitions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.key == 'simple_approval')]", hasSize(1)));
    }

    @Test
    void startInstanceThenCompleteSubmitAndApproveReachesApprovedEnd() throws Exception {
        String startBody = objectMapper.writeValueAsString(Map.of(
                "processDefinitionKey", "simple_approval",
                "businessKey", "TEST-BK-1",
                "variables", Map.of("approverUserId", adminUserId.toString())));

        String startResponse = mockMvc.perform(post("/api/workflow/instances/start")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processDefinitionKey").value("simple_approval"))
                .andReturn().getResponse().getContentAsString();
        String processInstanceId = objectMapper.readTree(startResponse).get("data").get("id").asText();

        // Task "Submit" duoc gan cho chinh nguoi start quy trinh (initiatorUserId) - la admin trong test nay.
        String submitTaskId = firstMyTaskId();
        mockMvc.perform(post("/api/workflow/tasks/" + submitTaskId + "/complete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // Task "Approve" duoc DynamicAssigneeTaskListener gan theo approverUserId - cung la admin.
        String approveTaskId = firstMyTaskId();
        mockMvc.perform(post("/api/workflow/tasks/" + approveTaskId + "/complete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("variables", Map.of("approved", true)))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/workflow/instances")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + processInstanceId + "')].endTime", hasSize(1)));
    }

    private String firstMyTaskId() throws Exception {
        String body = mockMvc.perform(get("/api/workflow/tasks/my")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data").get(0).get("id").asText();
    }
}
