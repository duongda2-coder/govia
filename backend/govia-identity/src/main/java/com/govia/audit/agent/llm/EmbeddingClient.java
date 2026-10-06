package com.govia.audit.agent.llm;

import com.govia.audit.agent.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Goi model embedding qua CUNG server voi provider chat dang dung (Ollama /api/embed hoac chuan OpenAI
 * /v1/embeddings). Moi loi (chua cau hinh model, server tat, model chua tai) deu tra Optional.empty()
 * thay vi nem loi - nguoi goi (AgentKnowledgeService) se lui ve tim theo tu khoa, tro ly van chay.
 */
@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    private final LlmProperties properties;
    private final RestClient restClient;

    public EmbeddingClient(LlmProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(properties.getEmbedding().getTimeoutSeconds());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        boolean openAi = isOpenAiCompatible();
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(openAi ? properties.getOpenaiCompatible().getBaseUrl() : properties.getOllama().getBaseUrl())
                .requestFactory(requestFactory);
        String apiKey = properties.getOpenaiCompatible().getApiKey();
        if (openAi && apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }
        LlmProperties.Ollama ollama = properties.getOllama();
        if (!openAi && ollama.getAccessClientId() != null && !ollama.getAccessClientId().isBlank()) {
            builder.defaultHeader("CF-Access-Client-Id", ollama.getAccessClientId());
            builder.defaultHeader("CF-Access-Client-Secret", ollama.getAccessClientSecret());
        }
        this.restClient = builder.build();
    }

    public boolean isConfigured() {
        String model = properties.getEmbedding().getModel();
        return model != null && !model.isBlank() && !"fake".equalsIgnoreCase(properties.getProvider());
    }

    public String modelId() {
        return properties.getEmbedding().getModel();
    }

    /** Tra ve 1 vector cho moi text dau vao (cung thu tu), hoac empty neu khong embed duoc. */
    public Optional<List<float[]>> embed(List<String> texts) {
        if (!isConfigured() || texts.isEmpty()) {
            return Optional.empty();
        }
        try {
            List<float[]> vectors = isOpenAiCompatible() ? embedOpenAi(texts) : embedOllama(texts);
            return vectors.size() == texts.size() ? Optional.of(vectors) : Optional.empty();
        } catch (Exception e) {
            log.warn("Khong goi duoc model embedding '{}': {}", modelId(), e.getMessage());
            return Optional.empty();
        }
    }

    private List<float[]> embedOllama(List<String> texts) {
        Map<String, Object> response = restClient.post()
                .uri("/api/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", modelId(), "input", texts))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        List<float[]> result = new ArrayList<>();
        if (response != null && response.get("embeddings") instanceof List<?> list) {
            list.forEach(v -> result.add(toVector(v)));
        }
        return result;
    }

    private List<float[]> embedOpenAi(List<String> texts) {
        Map<String, Object> response = restClient.post()
                .uri("/v1/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", modelId(), "input", texts))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        List<float[]> result = new ArrayList<>();
        if (response != null && response.get("data") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    result.add(toVector(m.get("embedding")));
                }
            }
        }
        return result;
    }

    private boolean isOpenAiCompatible() {
        return "openai-compatible".equalsIgnoreCase(properties.getProvider());
    }

    private static float[] toVector(Object raw) {
        if (!(raw instanceof List<?> values)) {
            return new float[0];
        }
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i) instanceof Number n ? n.floatValue() : 0f;
        }
        return vector;
    }
}
