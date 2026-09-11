package com.govia.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govia.audit.planengagement.dto.AuditEngagementResponse;
import com.govia.audit.planengagement.entity.AuditEngagementStatus;
import com.govia.audit.planengagement.monitoring.dto.AuditEngagementMonitoringResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @JsonUnwrapped tren 1 record long trong record khac la 1 to hop it dung - xac nhan Jackson thuc
 * su tra PHANG cac truong cua AuditEngagementResponse ra cung cap voi memberCount/... (khong bi
 * long thanh {"engagement": {...}, "memberCount": ...}), vi FE (AuditEngagementMonitoringItem)
 * dung truc tiep dang phang giong AuditEngagementItem + cac truong tong hop.
 */
class AuditEngagementMonitoringResponseSerializationTest {

    @Test
    void unwrapsEngagementFieldsFlatIntoJson() throws Exception {
        UUID id = UUID.randomUUID();
        AuditEngagementResponse engagement = new AuditEngagementResponse(id, "CN15002026001", UUID.randomUUID(), "DTKT01", "Chi nhanh test",
                "CN", 2026, 6, null, UUID.randomUUID(), "NV001", "Nguyen Van A", "QD01", AuditEngagementStatus.PLANNED, null, "Dot KT test",
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, "Tot", null, "nvA");

        AuditEngagementMonitoringResponse response = new AuditEngagementMonitoringResponse(engagement, 5, 3, 10, 2, 4);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(response);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = mapper.readValue(json, Map.class);

        assertThat(map).doesNotContainKey("engagement");
        assertThat(map.get("id")).isEqualTo(id.toString());
        assertThat(map.get("code")).isEqualTo("CN15002026001");
        assertThat(map.get("teamRanking")).isEqualTo("Tot");
        assertThat(map.get("memberCount")).isEqualTo(5);
        assertThat(map.get("businessSegmentCount")).isEqualTo(3);
        assertThat(map.get("totalFindings")).isEqualTo(10);
        assertThat(map.get("totalMaterialFindings")).isEqualTo(2);
        assertThat(map.get("recommendationCount")).isEqualTo(4);
    }
}
