package com.govia.audit.agent.service;

import com.govia.audit.agent.dto.AgentPageContext;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;

/**
 * A0 dieu phoi - chon agent chuyen trach cho moi cau hoi bang luat xac dinh (khong ton them 1 luot goi
 * model, ket qua lap lai duoc va test duoc), theo thu tu:
 * <ol>
 *   <li>Cau hoi co y dinh cua A0 (viec cua toi, man hinh, van ban/quy dinh...) -> A0, ke ca khi co
 *       nhac "rui ro" (vd "quy dinh ve cham diem rui ro" la cau hoi tra van ban).</li>
 *   <li>Agent chuyen trach khop nhieu tu khoa nhat -> agent do.</li>
 *   <li>Khong khop tu khoa nao (thuong la cau hoi tiep, vd "con nam 2024 thi sao?"): giu agent cua
 *       luot truoc; chua co luot truoc thi theo man hinh dang mo; cuoi cung la A0.</li>
 * </ol>
 * Agent dang bi tat duoc bo qua. Khi so agent tang (G2+) co the thay bang phan loai bang model nho.
 */
@Component
public class AgentRouter {

    private final AgentProfileRegistry registry;

    public AgentRouter(AgentProfileRegistry registry) {
        this.registry = registry;
    }

    public AgentProfile route(String message, AgentPageContext pageContext, String previousAgentCode) {
        AgentProfile general = registry.general();
        String text = AgentText.normalize(message);

        if (hits(general, text) > 0) {
            return general;
        }

        Optional<AgentProfile> bestSpecialist = registry.all().stream()
                .filter(p -> p != general && registry.isEnabled(p))
                .filter(p -> hits(p, text) > 0)
                .max(Comparator.comparingLong(p -> hits(p, text)));
        if (bestSpecialist.isPresent()) {
            return bestSpecialist.get();
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
                .filter(p -> p.routingPathPrefixes().stream().anyMatch(path::startsWith))
                .findFirst()
                .orElse(general);
    }

    private static long hits(AgentProfile profile, String normalizedText) {
        return profile.routingKeywords().stream().filter(k -> AgentText.containsPhrase(normalizedText, k)).count();
    }
}
