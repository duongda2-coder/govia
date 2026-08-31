package com.govia.audit.agent.llm;

import com.govia.audit.agent.tools.AuditToolRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * LlmProvider gia lap dung cho test (kich hoat qua govia.llm.provider=fake trong
 * application-test.yml) - KHONG goi Ollama that, tra ve dung kich ban da script san qua
 * {@link #enqueue}. Moi test method PHAI goi {@link #reset()} truoc khi script de tranh dinh vao
 * kich ban con lai cua test truoc (context Spring duoc tai su dung giua cac test trong cung class).
 */
@Component
@ConditionalOnProperty(name = "govia.llm.provider", havingValue = "fake")
public class FakeLlmProvider implements LlmProvider {

    private final Deque<ChatResult> script = new ArrayDeque<>();
    private boolean available = true;

    public void reset() {
        script.clear();
        available = true;
    }

    public void enqueueToolCall(String toolName, Map<String, Object> arguments) {
        script.addLast(new ChatResult(null, List.of(new ToolCallRequest(null, toolName, arguments))));
    }

    public void enqueueFinalAnswer(Map<String, Object> finalAnswerArguments) {
        script.addLast(new ChatResult(null, List.of(new ToolCallRequest(null, AuditToolRegistry.FINAL_ANSWER_TOOL_NAME, finalAnswerArguments))));
    }

    public void enqueueFreeText(String content) {
        script.addLast(new ChatResult(ChatMessage.assistant(content), List.of()));
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public ChatResult chat(List<ChatMessage> messages, List<ToolSpec> tools) {
        if (script.isEmpty()) {
            return new ChatResult(null, List.of(new ToolCallRequest(null, AuditToolRegistry.FINAL_ANSWER_TOOL_NAME,
                    Map.of("answer", "Hien chua co du lieu trong he thong de ket luan.", "facts", List.of()))));
        }
        return script.pollFirst();
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String modelId() {
        return "fake-test-model";
    }
}
