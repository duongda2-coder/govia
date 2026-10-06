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
 * LlmProvider cho moi server noi API chuan OpenAI "/v1/chat/completions" - muc tieu chinh la vLLM
 * chay model ~30B tren may GPU noi bo (on-premise), du lieu khong ra khoi ha tang ngan hang. Bat bang
 * govia.llm.provider=openai-compatible; AgentOrchestratorService khong doi gi.
 *
 * <p>Khac Ollama: chuan OpenAI bat buoc moi tool_call co "id" va message "tool" tra ket qua phai
 * mang dung "tool_call_id" do. ChatMessage dung chung khong luu id, nen khi doi sang dinh dang
 * OpenAI ta ghep cap theo thu tu: AgentOrchestratorService luon them [assistant(1 tool call),
 * tool(ket qua cua chinh call do)] lien nhau, nen message "tool" nhan id cua tool call ngay truoc no.
 */
@Component
@ConditionalOnProperty(name = "govia.llm.provider", havingValue = "openai-compatible")
public class OpenAiCompatibleProvider implements LlmProvider {

    private final RestClient restClient;
    private final LlmProperties.OpenAiCompatible config;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleProvider(LlmProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getOpenaiCompatible();
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(config.getTimeoutSeconds());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .requestFactory(requestFactory);
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + config.getApiKey());
        }
        this.restClient = builder.build();
    }

    @Override
    public ChatResult chat(List<ChatMessage> messages, List<ToolSpec> tools) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModel());
        body.put("messages", toOpenAiMessages(messages));
        if (!tools.isEmpty()) {
            body.put("tools", tools.stream().map(this::toOpenAiTool).toList());
            body.put("tool_choice", "auto");
        }
        body.put("temperature", 0.1);
        body.put("stream", false);

        Map<String, Object> response = restClient.post()
                .uri("/v1/chat/completions")
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
            Map<String, Object> models = restClient.get()
                    .uri("/v1/models")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (models == null || !(models.get("data") instanceof List<?> data)) {
                return false;
            }
            return data.stream().anyMatch(m -> m instanceof Map<?, ?> mm && config.getModel().equals(String.valueOf(mm.get("id"))));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String modelId() {
        return config.getModel();
    }

    private List<Map<String, Object>> toOpenAiMessages(List<ChatMessage> messages) {
        List<Map<String, Object>> result = new ArrayList<>();
        String lastToolCallId = null;
        int generatedIds = 0;
        for (ChatMessage message : messages) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", message.role());
            if (message.toolCalls() != null && !message.toolCalls().isEmpty()) {
                List<Map<String, Object>> calls = new ArrayList<>();
                for (ToolCallRequest call : message.toolCalls()) {
                    String id = call.id() != null ? call.id() : "call_" + (++generatedIds);
                    lastToolCallId = id;
                    calls.add(toOpenAiToolCall(id, call));
                }
                m.put("content", message.content());
                m.put("tool_calls", calls);
            } else if ("tool".equals(message.role())) {
                m.put("tool_call_id", lastToolCallId != null ? lastToolCallId : "call_unknown");
                m.put("content", message.content() == null ? "" : message.content());
            } else {
                m.put("content", message.content() == null ? "" : message.content());
            }
            result.add(m);
        }
        return result;
    }

    private Map<String, Object> toOpenAiToolCall(String id, ToolCallRequest call) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", call.name());
        try {
            function.put("arguments", objectMapper.writeValueAsString(call.arguments() == null ? Map.of() : call.arguments()));
        } catch (Exception e) {
            function.put("arguments", "{}");
        }
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("id", id);
        wrapper.put("type", "function");
        wrapper.put("function", function);
        return wrapper;
    }

    private Map<String, Object> toOpenAiTool(ToolSpec tool) {
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
        if (response == null || !(response.get("choices") instanceof List<?> choices) || choices.isEmpty()
                || !(choices.get(0) instanceof Map<?, ?> choice) || !(choice.get("message") instanceof Map<?, ?> messageRaw)) {
            return new ChatResult(ChatMessage.assistant("Khong nhan duoc phan hoi tu LLM server."), List.of());
        }
        Map<String, Object> message = (Map<String, Object>) messageRaw;
        Object rawToolCalls = message.get("tool_calls");
        if (rawToolCalls instanceof List<?> list && !list.isEmpty()) {
            List<ToolCallRequest> calls = new ArrayList<>();
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> callMap) || !(callMap.get("function") instanceof Map<?, ?> function)) {
                    continue;
                }
                Object id = callMap.get("id");
                calls.add(new ToolCallRequest(id == null ? null : String.valueOf(id), String.valueOf(function.get("name")),
                        normalizeArguments(function.get("arguments"))));
            }
            return new ChatResult(null, calls);
        }
        Object content = message.get("content");
        return new ChatResult(ChatMessage.assistant(content == null ? "" : String.valueOf(content)), List.of());
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
