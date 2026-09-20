package com.govia.identity;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Di qua controller that (@PreAuthorize + rang buoc tham so) cho 2 nut "Xuat QD thanh lap doan" / "Xuat QD kiem ke" cua KHNS_NAM -
 * test service goi thang khong bat duoc loi o annotation/tham so cua endpoint. */
class AuditKhnsNamDecisionApiTest extends AbstractApiTest {

    @Test
    void exportDecisionIsReachableAndReportsMissingTeamAsBusinessError() throws Exception {
        for (String type : new String[]{"TEAM", "INVENTORY"}) {
            mockMvc.perform(get("/api/audit/plan/khns-nam/export-decision")
                            .param("year", "2099").param("month", "3").param("auditObjectCode", "NO-SUCH").param("type", type)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("KHNS_DECISION_NO_TEAM"));
        }
    }

    @Test
    void exportDecisionRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/audit/plan/khns-nam/export-decision")
                        .param("year", "2099").param("month", "3").param("auditObjectCode", "X").param("type", "TEAM"))
                .andExpect(status().is4xxClientError());
    }
}
