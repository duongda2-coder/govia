package com.govia.audit.agent.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.govia.audit.agent.config.LlmProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LlmProvider chay Ollama local qua native tool-calling ("/api/chat" voi field "tools") - KHONG bao
 * gio goi OpenAI/Anthropic/Gemini. URL/model 100% tu LlmProperties (khong hardcode). Neu sau nay them
 * provider khac (OpenAI/Claude), chi can 1 class moi implement LlmProvider - AgentOrchestratorService
 * khong doi.
 */
@Component
@ConditionalOnProperty(name = "govia.llm.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaProvider implements LlmProvider {

    private final RestClient restClient;
    private final LlmProperties.Ollama config;
    private final ObjectMapper objectMapper;

    public OllamaProvider(LlmProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getOllama();
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(config.getTimeoutSeconds());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .requestFactory(requestFactory);
        if (config.getAccessClientId() != null && !config.getAccessClientId().isBlank()) {
            builder.defaultHeader("CF-Access-Client-Id", config.getAccessClientId());
            builder.defaultHeader("CF-Access-Client-Secret", config.getAccessClientSecret());
        }
        this.restClient = builder.build();
    }

    @Override
    public ChatResult chat(List<ChatMessage> messages, List<ToolSpec> tools) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModel());
        body.put("messages", messages.stream().map(this::toOllamaMessage).toList());
        body.put("tools", tools.stream().map(this::toOllamaTool).toList());
        body.put("stream", false);

        Map<String, Object> response = restClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return parseResponse(response);
    }

    @Override
    public boolean isAvailable() {
        try {
            Map<String, Object> tags = restClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (tags == null || !(tags.get("models") instanceof List<?> models)) {
                return false;
            }
            return models.stream().anyMatch(m -> m instanceof Map<?, ?> mm && config.getModel().equals(String.valueOf(mm.get("name"))));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String modelId() {
        return config.getModel();
    }

    private Map<String, Object> toOllamaMessage(ChatMessage message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", message.role());
        m.put("content", message.content() == null ? "" : message.content());
        if (message.toolCalls() != null && !message.toolCalls().isEmpty()) {
            m.put("tool_calls", message.toolCalls().stream().map(this::toOllamaToolCall).toList());
        }
        return m;
    }

    private Map<String, Object> toOllamaToolCall(ToolCallRequest call) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", call.name());
        function.put("arguments", call.arguments());
        return Map.of("function", function);
    }

    private Map<String, Object> toOllamaTool(ToolSpec tool) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", tool.name());
        function.put("description", tool.description());
        function.put("parameters", tool.parametersJsonSchema());
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("type", "function");
        wrapper.put("function", function);
        return wrapper;
    }

    @SuppressWarnings("unchecked")
    private ChatResult parseResponse(Map<String, Object> response) {
        if (response == null || !(response.get("message") instanceof Map<?, ?> messageRaw)) {
            return new ChatResult(ChatMessage.assistant("Khong nhan duoc phan hoi tu Ollama."), List.of());
        }
        Map<String, Object> message = (Map<String, Object>) messageRaw;
        Object rawToolCalls = message.get("tool_calls");
        if (rawToolCalls instanceof List<?> list && !list.isEmpty()) {
            List<ToolCallRequest> calls = new ArrayList<>();
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> callMap) || !(callMap.get("function") instanceof Map<?, ?> function)) {
                    continue;
                }
                String name = String.valueOf(function.get("name"));
                Map<String, Object> arguments = normalizeArguments(function.get("arguments"));
                calls.add(new ToolCallRequest(null, name, arguments));
            }
            return new ChatResult(null, calls);
        }
        String content = String.valueOf(message.getOrDefault("content", ""));
        return new ChatResult(ChatMessage.assistant(content), List.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeArguments(Object raw) {
        if (raw instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        if (raw instanceof String s && !s.isBlank()) {
            try {
                return objectMapper.readValue(s, new TypeReference<>() {
                });
            } catch (Exception e) {
                return Map.of();
            }
        }
        return Map.of();
    }
}
