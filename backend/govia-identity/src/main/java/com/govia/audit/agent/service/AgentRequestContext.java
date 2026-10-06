package com.govia.audit.agent.service;

import com.govia.audit.agent.dto.AgentScreenRef;

import java.util.List;

/**
 * Du lieu kem theo 1 request chat ma tool can doc (hien chi co danh sach man hinh nguoi dung duoc thay,
 * cho search_screens). Giu theo thread vi ca luot chat chay dong bo tren 1 thread (xem
 * AuditToolExecutor) - AgentOrchestratorService dat vao dau luot va LUON xoa o finally.
 */
public final class AgentRequestContext {

    private static final ThreadLocal<List<AgentScreenRef>> SCREENS = new ThreadLocal<>();

    private AgentRequestContext() {
    }

    public static void setScreens(List<AgentScreenRef> screens) {
        SCREENS.set(screens == null ? List.of() : screens.stream().filter(java.util.Objects::nonNull).toList());
    }

    public static List<AgentScreenRef> screens() {
        List<AgentScreenRef> screens = SCREENS.get();
        return screens == null ? List.of() : screens;
    }

    public static void clear() {
        SCREENS.remove();
    }
}
