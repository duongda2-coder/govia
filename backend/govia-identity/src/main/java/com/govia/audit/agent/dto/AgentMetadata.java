package com.govia.audit.agent.dto;

import java.time.Instant;
import java.util.List;

/** grounded = false neu evidence-guard da loai bo it nhat 1 evidence khong co can cu - frontend nen
 * hien canh bao nhe khi false thay vi coi cau tra loi hoan toan dang tin cay nhu binh thuong. */
public record AgentMetadata(String model, Instant timestamp, List<String> toolsUsed, boolean truncated, boolean grounded) {
}
