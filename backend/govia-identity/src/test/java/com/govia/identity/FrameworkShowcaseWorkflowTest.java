package com.govia.identity;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiem chung cac kha nang BPM chua duoc dung o simple_approval/employee_approval, tren quy trinh
 * "framework_showcase" (xem framework-showcase.bpmn20.xml): Parallel Fork/Join, embedded Sub-process,
 * Service Task async + Retry, Service Task + Boundary Error Event, Signal Event, cong voi cac kha nang
 * dung chung khac cua ha tang Workflow: Versioning (definition), Subtask, SLA (due date/overdue).
 *
 * Ghi de @Transactional(NOT_SUPPORTED) cua AbstractApiTest: cac test o day can du lieu THUC SU
 * COMMIT (khong roll back cuoi test) vi Flowable async job executor chay tren THREAD RIENG, dung
 * connection/transaction khac - neu con nam trong transaction test (chua commit) thi thread do
 * khong thay duoc du lieu, retry se khong bao gio tien trien duoc.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FrameworkShowcaseWorkflowTest extends AbstractApiTest {

    @Test
    void successPathExercisesParallelSubprocessRetryAndSignal() throws Exception {
        String processInstanceId = startShowcase(false);

        completeAllOpenTasksByName(processInstanceId, "Task A (song song)", "Task B (song song)");
        completeAllOpenTasksByName(processInstanceId, "Duyệt trong sub-process");

        waitUntilEnded(processInstanceId, Duration.ofSeconds(45));

        String historyBody = mockMvc.perform(get("/api/workflow/instances/" + processInstanceId + "/history")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode history = objectMapper.readTree(historyBody).get("data");

        List<String> activityIds = new ArrayList<>();
        for (JsonNode a : history.get("activities")) {
            activityIds.add(a.get("activityId").asText());
        }
        assertThat(activityIds).contains("taskA", "taskB", "subReviewTask", "retryServiceTask",
                "riskyServiceTask", "signalThrow", "endShowcase");
        assertThat(activityIds).doesNotContain("errorHandledEnd");

        Map<String, Boolean> variables = new java.util.HashMap<>();
        for (JsonNode v : history.get("variables")) {
            if (v.get("value").isBoolean()) {
                variables.put(v.get("name").asText(), v.get("value").asBoolean());
            }
        }
        assertThat(variables.get("retryServiceTaskSucceeded")).isTrue();
        assertThat(variables.get("riskyServiceTaskSucceeded")).isTrue();
    }

    @Test
    void errorPathIsCaughtByBoundaryErrorEvent() throws Exception {
        String processInstanceId = startShowcase(true);

        completeAllOpenTasksByName(processInstanceId, "Task A (song song)", "Task B (song song)");
        completeAllOpenTasksByName(processInstanceId, "Duyệt trong sub-process");

        waitUntilEnded(processInstanceId, Duration.ofSeconds(45));

        String historyBody = mockMvc.perform(get("/api/workflow/instances/" + processInstanceId + "/history")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode history = objectMapper.readTree(historyBody).get("data");

        List<String> activityIds = new ArrayList<>();
        for (JsonNode a : history.get("activities")) {
            activityIds.add(a.get("activityId").asText());
        }
        assertThat(activityIds).contains("riskyServiceTask", "riskyErrorBoundary", "errorHandledEnd");
        assertThat(activityIds).doesNotContain("signalThrow", "endShowcase");
    }

    @Test
    void processDefinitionVersioningTracksRedeployment() throws Exception {
        String versionsBody = mockMvc.perform(get("/api/workflow/process-definitions/simple_approval/versions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        int versionsBefore = objectMapper.readTree(versionsBody).get("data").size();
        assertThat(versionsBefore).isGreaterThanOrEqualTo(1);

        byte[] bpmn = getClass().getClassLoader()
                .getResourceAsStream("processes-templates/simple-approval.bpmn20.xml").readAllBytes();
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "simple-approval.bpmn20.xml", "text/xml", bpmn);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/workflow/process-definitions/deploy")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        String versionsAfterBody = mockMvc.perform(get("/api/workflow/process-definitions/simple_approval/versions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode versionsAfter = objectMapper.readTree(versionsAfterBody).get("data");
        assertThat(versionsAfter.size()).isEqualTo(versionsBefore + 1);

        String oldDefinitionId = versionsAfter.get(1).get("id").asText();
        mockMvc.perform(post("/api/workflow/process-definitions/" + oldDefinitionId + "/suspend")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/workflow/process-definitions/" + oldDefinitionId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void subtaskAndSlaDueDateWork() throws Exception {
        String processInstanceId = startShowcase(false);
        String taskAId = findOpenTaskIdByName(processInstanceId, "Task A (song song)");

        String subtaskBody = mockMvc.perform(post("/api/workflow/tasks/" + taskAId + "/subtasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Xac minh ho so"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentTaskId").value(taskAId))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String subtaskId = objectMapper.readTree(subtaskBody).get("data").get("id").asText();

        mockMvc.perform(get("/api/workflow/tasks/" + taskAId + "/subtasks")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + subtaskId + "')]", org.hamcrest.Matchers.hasSize(1)));

        Instant pastDue = Instant.now().minus(Duration.ofDays(1));
        mockMvc.perform(put("/api/workflow/tasks/" + taskAId + "/due-date")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("dueDate", pastDue.toString()))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/workflow/tasks/overdue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + taskAId + "')]", org.hamcrest.Matchers.hasSize(1)));

        // Don dep: subtask la task doc lap gan truc tiep cho admin (khong thuoc process instance nen
        // /cancel ben duoi khong xoa duoc) - neu de mo se ton tai that su (class nay @Transactional
        // NOT_SUPPORTED, khong rollback) va lam "no le" sang /api/workflow/tasks/my cua admin o CAC
        // TEST CLASS KHAC chay sau trong cung JVM (vd EmployeeApprovalWorkflowTest mong doi task dau
        // tien tra ve la task no vua tao, khong phai subtask con sot lai nay).
        mockMvc.perform(post("/api/workflow/tasks/" + subtaskId + "/complete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/workflow/instances/" + processInstanceId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private String startShowcase(boolean simulateRiskyError) throws Exception {
        String body = mockMvc.perform(post("/api/workflow/instances/start")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "processDefinitionKey", "framework_showcase",
                                "variables", Map.of("simulateRiskyError", simulateRiskyError)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("data").get("id").asText();
    }

    private void completeAllOpenTasksByName(String processInstanceId, String... names) throws Exception {
        for (String name : names) {
            String taskId = findOpenTaskIdByName(processInstanceId, name);
            mockMvc.perform(post("/api/workflow/tasks/" + taskId + "/complete")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }
    }

    private String findOpenTaskIdByName(String processInstanceId, String name) throws Exception {
        String body = mockMvc.perform(get("/api/workflow/tasks")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        for (JsonNode task : objectMapper.readTree(body).get("data")) {
            if (processInstanceId.equals(task.get("processInstanceId").asText()) && name.equals(task.get("name").asText())) {
                return task.get("id").asText();
            }
        }
        throw new AssertionError("Khong tim thay task '" + name + "' dang mo cho processInstanceId=" + processInstanceId);
    }

    private void waitUntilEnded(String processInstanceId, Duration timeout) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            String body = mockMvc.perform(get("/api/workflow/instances")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            for (JsonNode instance : objectMapper.readTree(body).get("data")) {
                if (processInstanceId.equals(instance.get("id").asText()) && !instance.get("endTime").isNull()) {
                    return;
                }
            }
            Thread.sleep(300);
        }
        throw new AssertionError("Process instance " + processInstanceId + " khong ket thuc trong " + timeout);
    }
}
