package com.govia.identity.workflow.dto;

import java.util.List;

public record ProcessInstanceHistoryDetail(
        ProcessInstanceSummary instance,
        List<ActivityHistoryEntry> activities,
        List<VariableHistoryEntry> variables
) {
}
