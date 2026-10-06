package com.govia.audit.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** "AI chuẩn hoá câu chữ" - viet lai doan van ban nguoi dung da nhap, giu nguyen noi dung. */
public record RewriteRequest(
        @NotBlank @Size(max = 4000) String text,
        @NotNull Purpose purpose
) {
    public enum Purpose { RECOMMENDATION, FINDING, SUMMARY }
}
