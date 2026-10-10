package com.govia.audit.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * G4 - gioi han tro ly chuyen trach theo VAI TRO (bang cau hinh, khong sua vai tro/quyen hien co). Dang
     * "A6=TRUONG_DOAN|KSCL;A2=KE_HOACH": chi nguoi co 1 trong cac vai tro do moi dung duoc A6/A2 (SUPER_ADMIN
     * luon duoc). Trong = khong gioi han. Agent khong duoc dung thi cau hoi chuyen ve A0, cong cu soan nhap an di.
     */
    private String agentRoles = "";

    public String getAgentRoles() {
        return agentRoles;
    }

    public void setAgentRoles(String agentRoles) {
        this.agentRoles = agentRoles;
    }

    /** Vai tro duoc phep dung agent; rong = khong gioi han. */
    public Set<String> rolesFor(String agentCode) {
        if (agentRoles == null || agentRoles.isBlank() || agentCode == null) {
            return Set.of();
        }
        for (String part : agentRoles.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(agentCode)) {
                return Arrays.stream(kv[1].split("[|,]")).map(String::trim).filter(r -> !r.isEmpty())
                        .map(r -> r.toUpperCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
            }
        }
        return Set.of();
    }

    private final Schedule schedule = new Schedule();

    public Schedule getSchedule() {
        return schedule;
    }

    /**
     * G4 (muc M3) - job "Gợi ý AI" chay theo lich RIENG cua module agent (Spring @Scheduled, khong them timer vao
     * BPMN). Chi doc du lieu bang quyen cua tung nguoi dung, ghi ket qua vao bang agent_suggestion - khong tao task
     * Flowable, khong gui thu, khong goi model.
     */
    public static class Schedule {
        private boolean enabled = true;
        private String cron = "0 0 6 * * MON-FRI";
        private int dueSoonDays = 7;
        private int staleTaskDays = 3;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public int getDueSoonDays() {
            return dueSoonDays;
        }

        public void setDueSoonDays(int dueSoonDays) {
            this.dueSoonDays = dueSoonDays;
        }

        public int getStaleTaskDays() {
            return staleTaskDays;
        }

        public void setStaleTaskDays(int staleTaskDays) {
            this.staleTaskDays = staleTaskDays;
        }
    }

    public boolean isAgentEnabled(String agentCode) {
        return enabled && disabledAgents.stream().noneMatch(code -> code.equalsIgnoreCase(agentCode));
    }
}
