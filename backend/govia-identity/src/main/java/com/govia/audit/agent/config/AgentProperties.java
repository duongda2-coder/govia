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

    private final FileReading fileReading = new FileReading();

    public FileReading getFileReading() {
        return fileReading;
    }

    /**
     * Cong 3 trong phuong an: AI DOC NOI DUNG FILE dinh kem. MAC DINH TAT - chi bat (GOVIA_AGENT_FILE_READING_ENABLED
     * =true) sau khi ATTT phe duyet. Tat thi tool doc file khong duoc dua cho model, RAG chi dung metadata.
     */
    public static class FileReading {
        private boolean enabled = false;
        private long maxBytes = 10L * 1024 * 1024;
        private int maxChars = 20_000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getMaxBytes() {
            return maxBytes;
        }

        public void setMaxBytes(long maxBytes) {
            this.maxBytes = maxBytes;
        }

        public int getMaxChars() {
            return maxChars;
        }

        public void setMaxChars(int maxChars) {
            this.maxChars = maxChars;
        }
    }

    public boolean isAgentEnabled(String agentCode) {
        return enabled && disabledAgents.stream().noneMatch(code -> code.equalsIgnoreCase(agentCode));
    }
}
