package com.govia.identity.workflow.dto;

public record ProcessDefinitionSummary(String id, String key, String name, int version, boolean suspended) {
}
