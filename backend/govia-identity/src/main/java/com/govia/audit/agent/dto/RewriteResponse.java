package com.govia.audit.agent.dto;

import java.util.List;

/** grounded=false: ban viet lai co so lieu khong co trong van ban goc - giao dien canh bao, nguoi dung
 * van quyet dinh co dung hay khong. */
public record RewriteResponse(String text, List<String> changes, boolean grounded, String model) {
}
