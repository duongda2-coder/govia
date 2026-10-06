package com.govia.audit.agent.dto;

import jakarta.validation.constraints.Size;

/** Man hinh nguoi dung dang mo khi hoi - giup tro ly hieu "man hinh nay", "chi nhanh dang xem"... */
public record AgentPageContext(
        @Size(max = 300) String path,
        @Size(max = 300) String screenLabel,
        @Size(max = 300) String groupLabel
) {
}
