package com.govia.audit.agent.dto;

import jakarta.validation.constraints.Size;

/** 1 muc menu nguoi dung duoc thay (label/nhom/duong dan) - frontend gui kem moi luot chat. */
public record AgentScreenRef(
        @Size(max = 300) String label,
        @Size(max = 300) String group,
        @Size(max = 300) String path
) {
}
