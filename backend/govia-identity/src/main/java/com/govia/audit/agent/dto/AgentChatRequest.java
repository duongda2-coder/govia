package com.govia.audit.agent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** conversationId do frontend tu sinh (giu trong sessionStorage) de gop nhieu luot hoi/dap thanh 1
 * hoi thoai co ngu canh - hoi thoai duoc luu DB (agent_conversation) nen mo lai van xem tiep duoc.
 * pageContext: man hinh nguoi dung dang mo (tuy chon, nguoi dung bo duoc tren giao dien).
 * screens: danh sach man hinh ma CHINH nguoi dung nay duoc thay tren menu (frontend da loc theo quyen)
 * - la nguon cho tool search_screens, nen tro ly khong bao gio chi toi man hinh nguoi dung khong co quyen. */
public record AgentChatRequest(
        @NotNull UUID conversationId,
        @NotBlank @Size(max = 4000) String message,
        @Valid AgentPageContext pageContext,
        @Size(max = 500) List<@Valid AgentScreenRef> screens
) {
}
