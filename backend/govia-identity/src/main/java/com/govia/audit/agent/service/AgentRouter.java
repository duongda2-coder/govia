package com.govia.audit.agent.service;

import com.govia.audit.agent.dto.AgentPageContext;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;

/**
 * A0 dieu phoi - chon agent chuyen trach cho moi cau hoi bang luat xac dinh (khong ton them 1 luot goi
 * model, ket qua lap lai duoc va test duoc):
 * <ol>
 *   <li>Cham diem moi agent = tong so tu (am tiet) cua cac cum tu khoa khop - cum dai, cu the ("diem
 *       kiem soat") thang cum ngan, chung ("diem"). Tu khoa cua A0 (viec cua toi, man hinh, van ban/quy
 *       dinh...) nhan he so {@link #GENERAL_WEIGHT} vi do la Y DINH ro rang cua cau hoi - vd "quy dinh ve
 *       cham diem rui ro" la cau hoi tra van ban, khong phai hoi diem.</li>
 *   <li>Diem cao nhat thang; hoa nhau thi theo thu tu trong AgentProfileRegistry.</li>
 *   <li>Khong khop tu khoa nao (thuong la cau hoi tiep, vd "con nam 2024 thi sao?"): giu agent cua
 *       luot truoc; chua co luot truoc thi theo man hinh dang mo (duong dan khop dai nhat); cuoi cung A0.</li>
 * </ol>
 * Agent dang bi tat duoc bo qua.
 */
@Component
public class AgentRouter {

    static final int GENERAL_WEIGHT = 3;

    private final AgentProfileRegistry registry;

    public AgentRouter(AgentProfileRegistry registry) {
        this.registry = registry;
    }

    public AgentProfile route(String message, AgentPageContext pageContext, String previousAgentCode) {
        AgentProfile general = registry.general();
        String text = AgentText.normalize(message);

        AgentProfile best = null;
        long bestScore = 0;
        for (AgentProfile profile : registry.all()) {
            if (!registry.isEnabled(profile)) {
                continue;
            }
            long score = score(profile, text) * (profile == general ? GENERAL_WEIGHT : 1);
            if (score > bestScore) {
                best = profile;
                bestScore = score;
            }
        }
        if (best != null) {
            return best;
        }

        if (previousAgentCode != null) {
            Optional<AgentProfile> previous = registry.find(previousAgentCode).filter(registry::isEnabled);
            if (previous.isPresent()) {
                return previous.get();
            }
        }

        String path = pageContext == null || pageContext.path() == null ? "" : pageContext.path();
        return registry.all().stream()
                .filter(p -> p != general && registry.isEnabled(p))
                .flatMap(p -> p.routingPathPrefixes().stream().filter(path::startsWith).map(prefix -> new Object[]{p, prefix.length()}))
                .max(Comparator.comparingInt(pair -> (Integer) pair[1]))
                .map(pair -> (AgentProfile) pair[0])
                .orElse(general);
    }

    private static long score(AgentProfile profile, String normalizedText) {
        return profile.routingKeywords().stream()
                .filter(k -> AgentText.containsPhrase(normalizedText, k))
                .mapToLong(k -> AgentText.tokens(k).size())
                .sum();
    }
}
