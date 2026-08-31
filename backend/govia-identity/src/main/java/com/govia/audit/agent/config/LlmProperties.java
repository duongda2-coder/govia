package com.govia.audit.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Cau hinh chung cho lop LLM cua Audit AI Agent - khai bao duoi prefix govia.llm trong
 * application.yml. "provider" quyet dinh bean LlmProvider nao duoc dung (hien chi co "ollama"). */
@Component
@ConfigurationProperties(prefix = "govia.llm")
public class LlmProperties {

    private String provider = "ollama";

    private final Ollama ollama = new Ollama();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Ollama getOllama() {
        return ollama;
    }

    public static class Ollama {
        private String baseUrl = "http://localhost:11434";
        private String model = "qwen2.5:7b-instruct";
        private int timeoutSeconds = 120;
        /** Cloudflare Access service token - de trong khi chay local, chi dien khi Ollama duoc
         * expose qua Cloudflare Tunnel + Access cho moi truong remote (xem huong dan trien khai). */
        private String accessClientId = "";
        private String accessClientSecret = "";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public String getAccessClientId() {
            return accessClientId;
        }

        public void setAccessClientId(String accessClientId) {
            this.accessClientId = accessClientId;
        }

        public String getAccessClientSecret() {
            return accessClientSecret;
        }

        public void setAccessClientSecret(String accessClientSecret) {
            this.accessClientSecret = accessClientSecret;
        }
    }
}
