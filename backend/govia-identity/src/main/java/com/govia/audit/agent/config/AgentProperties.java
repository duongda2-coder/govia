package com.govia.audit.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Cau hinh van hanh cua AI Agent (prefix govia.agent) - tach rieng khoi LlmProperties (chi lo ket noi
 * model). "enabled" la CONG TAC TAT toan bo AI: false thi /chat tu choi, /health bao enabled=false va
 * frontend an nut AI - he thong nghiep vu chay y nhu chua tung co AI. "disabledAgents" tat rieng tung
 * agent chuyen trach (vd "A1"), cau hoi cua agent do duoc chuyen ve A0.
 */
@Component
@ConfigurationProperties(prefix = "govia.agent")
public class AgentProperties {

    private boolean enabled = true;

    private List<String> disabledAgents = new ArrayList<>();

    /** So message (user + assistant) gan nhat cua hoi thoai duoc dua lai cho model moi luot. */
    private int historyMessages = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getDisabledAgents() {
        return disabledAgents;
    }

    public void setDisabledAgents(List<String> disabledAgents) {
        this.disabledAgents = disabledAgents;
    }

    public int getHistoryMessages() {
        return historyMessages;
    }

    public void setHistoryMessages(int historyMessages) {
        this.historyMessages = historyMessages;
    }

    public boolean isAgentEnabled(String agentCode) {
        return enabled && disabledAgents.stream().noneMatch(code -> code.equalsIgnoreCase(agentCode));
    }
}
