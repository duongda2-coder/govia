package com.govia.audit.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Cau hinh chung cho lop LLM cua Audit AI Agent - khai bao duoi prefix govia.llm trong
 * application.yml. "provider" quyet dinh bean LlmProvider nao duoc dung: "ollama" (dev/may le),
 * "openai-compatible" (vLLM hoac server bat ky noi API /v1/chat/completions - moi truong van hanh)
 * hoac "fake" (test). */
@Component
@ConfigurationProperties(prefix = "govia.llm")
public class LlmProperties {

    private String provider = "ollama";

    private final Ollama ollama = new Ollama();

    private final OpenAiCompatible openaiCompatible = new OpenAiCompatible();

    private final Embedding embedding = new Embedding();

    public OpenAiCompatible getOpenaiCompatible() {
        return openaiCompatible;
    }

    public Embedding getEmbedding() {
        return embedding;
    }

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
        /** Nhiet do lay mau (giong provider vLLM = 0.1): thap de goi tool/so lieu on dinh. Mac dinh Ollama la 0.8. */
        private double temperature = 0.1;
        /** Gioi han so token model sinh ra moi luot (num_predict) - chan model nho "noi mai khong dung" toi het thoi gian cho. */
        private int maxTokens = 2048;
        /** Cua so ngu canh (num_ctx). Mac dinh Ollama 4096 qua nho cho prompt + bo tool + du lieu; 0 = dung mac dinh may chu. */
        private int contextSize = 8192;

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public int getContextSize() {
            return contextSize;
        }

        public void setContextSize(int contextSize) {
            this.contextSize = contextSize;
        }

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

    /** Server noi chuan OpenAI (vLLM la lua chon mac dinh cho moi truong van hanh on-premise). */
    public static class OpenAiCompatible {
        private String baseUrl = "http://localhost:8000";
        private String model = "";
        /** De trong neu server khong yeu cau (vLLM chay noi bo thuong khong bat api-key). */
        private String apiKey = "";
        private int timeoutSeconds = 120;

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

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    /**
     * Model embedding cho tim kiem ngu nghia (RAG) - goi qua CUNG server voi provider dang dung
     * (Ollama: /api/embed, openai-compatible: /v1/embeddings). model de trong = tat embedding, tim
     * kiem tai lieu tu dong lui ve so khop tu khoa - van chay duoc, chi kem chinh xac hon.
     */
    public static class Embedding {
        private String model = "";
        private int timeoutSeconds = 60;

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
    }
}
