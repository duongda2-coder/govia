package com.govia.identity;

import com.govia.identity.dto.EmployeeRequest;
import com.govia.identity.dto.EmployeeStatusRequest;
import com.govia.identity.dto.OrganizationUnitRequest;
import com.govia.identity.dto.PositionRequest;
import com.govia.identity.entity.EmployeeStatus;
import com.govia.identity.entity.Gender;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kich ban test cho ho so nhan vien: tao day du field moi (ngay sinh, gioi tinh, CCCD, email ca nhan, quan ly),
 * trung ma NV, don vi/quan ly khong ton tai, quan ly la chinh minh, vong lap bao cao,
 * tim kiem + phan trang, doi trang thai, xuat Excel/Word, chan truy cap khong token.
 */
class EmployeeApiTest extends AbstractApiTest {

    @Test
    void createEmployeeWithFullProfile() throws Exception {
        UUID orgUnitId = createOrgUnit("EMP-ORG-01");
        UUID positionId = createPosition("KY-SU-01", "Ky su");

        EmployeeRequest request = new EmployeeRequest("NV-1001", "Nguyen Van A", "a@govia.local",
                "a.canhan@gmail.com", "0901234567", orgUnitId, positionId, LocalDate.of(2024, 1, 15),
                LocalDate.of(1995, 5, 20), Gender.MALE, "079095001234", null, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null);

        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeCode").value("NV-1001"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.orgUnitName").value(notNullValue()))
                .andExpect(jsonPath("$.data.positionName").value("Ky su"))
                .andExpect(jsonPath("$.data.personalEmail").value("a.canhan@gmail.com"))
                .andExpect(jsonPath("$.data.dateOfBirth").value("1995-05-20"))
                .andExpect(jsonPath("$.data.gender").value("MALE"))
                .andExpect(jsonPath("$.data.idNumber").value("079095001234"));
    }

    @Test
    void duplicateEmployeeCodeIsRejected() throws Exception {
        createEmployee("NV-DUP", "Nguyen Van B", null, null);

        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest("NV-DUP", "Nguyen Van C", null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("EMPLOYEE_CODE_DUPLICATE"));
    }

    @Test
    void unknownOrgUnitIsRejected() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                employeeRequest("NV-ORG-X", "Nguyen Van D", UUID.randomUUID(), null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ORG_UNIT_NOT_FOUND"));
    }

    @Test
    void unknownManagerIsRejected() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                employeeRequest("NV-MGR-X", "Nguyen Van E", null, UUID.randomUUID()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("EMPLOYEE_MANAGER_NOT_FOUND"));
    }

    @Test
    void managerCannotBeSelf() throws Exception {
        UUID id = createEmployee("NV-SELF", "Nguyen Van F", null, null);

        mockMvc.perform(put("/api/employees/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest("NV-SELF", "Nguyen Van F", null, id))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("EMPLOYEE_INVALID_MANAGER"));
    }

    @Test
    void circularManagerChainIsRejected() throws Exception {
        UUID managerA = createEmployee("NV-A", "Quan ly A", null, null);
        UUID staffB = createEmployee("NV-B", "Nhan vien B", null, managerA);

        // B dang bao cao cho A. Thu doi A bao cao cho B -> phai bi tu choi vi tao vong lap
        mockMvc.perform(put("/api/employees/" + managerA)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                employeeRequest("NV-A", "Quan ly A", null, staffB))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("EMPLOYEE_MANAGER_CIRCULAR"));
    }

    @Test
    void listSupportsKeywordSearchAndPagination() throws Exception {
        createEmployee("NV-SEARCH-1", "Tran Thi Search", null, null);
        createEmployee("NV-OTHER-2", "Le Van Khac", null, null);

        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("keyword", "Search")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].employeeCode").value("NV-SEARCH-1"));
    }

    @Test
    void changeStatusUpdatesEmployee() throws Exception {
        UUID id = createEmployee("NV-STATUS", "Pham Van G", null, null);

        mockMvc.perform(patch("/api/employees/" + id + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmployeeStatusRequest(EmployeeStatus.ON_LEAVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ON_LEAVE"));
    }

    @Test
    void exportExcelReturnsExcelFile() throws Exception {
        createEmployee("NV-EXPORT-1", "Xuat Excel Test", null, null);

        mockMvc.perform(get("/api/employees/export/excel").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void exportWordReturnsWordFile() throws Exception {
        createEmployee("NV-EXPORT-2", "Xuat Word Test", null, null);

        mockMvc.perform(get("/api/employees/export/word").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().is4xxClientError());
    }

    private UUID createOrgUnit(String code) throws Exception {
        OrganizationUnitRequest request = new OrganizationUnitRequest(code, "Org " + code, null, "001", null, null);
        String body = mockMvc.perform(post("/api/org-units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("data").get("id").asText());
    }

    private UUID createPosition(String code, String name) throws Exception {
        PositionRequest request = new PositionRequest(code, name);
        String body = mockMvc.perform(post("/api/positions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("data").get("id").asText());
    }

    private UUID createEmployee(String code, String fullName, UUID orgUnitId, UUID managerId) throws Exception {
        String body = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest(code, fullName, orgUnitId, managerId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("data").get("id").asText());
    }

    private EmployeeRequest employeeRequest(String code, String fullName, UUID orgUnitId, UUID managerId) {
        return new EmployeeRequest(code, fullName, null, null, null, orgUnitId, null, null, null, null, null, managerId, null,
                null, null, null, null, null, null, null, null, null, null, false, null, null, null, false, null);
    }
}
