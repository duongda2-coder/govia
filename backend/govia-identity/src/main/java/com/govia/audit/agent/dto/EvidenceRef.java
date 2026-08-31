package com.govia.audit.agent.dto;

import java.util.Map;

/** 1 evidence trong cau tra loi cuoi - "tool" phai la 1 trong 10 Audit Tools THAT SU da duoc goi
 * trong luot nay (kiem tra boi AgentOrchestratorService.guardEvidence, khong chi tin loi model). */
public record EvidenceRef(String tool, Map<String, Object> args, Map<String, Object> keyData) {
}
