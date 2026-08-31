package com.govia.audit.agent.service;

import com.govia.audit.agent.llm.ChatMessage;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Luu hoi thoai TRONG BO NHO (khong ghi DB) - pham vi MVP da thong nhat: mat khi restart backend,
 * chap nhan duoc cho muc dich test noi bo. sessionId (conversationId) do frontend tu sinh moi khi mo
 * man chat. Chi giu toi da {@link #MAX_MESSAGES} message gan nhat de khong lam qua tai context cua
 * model local (7B, context window khong lon nhu model cloud).
 */
@Component
public class ConversationStore {

    private static final int MAX_MESSAGES = 20;
    private static final long STALE_AFTER_MILLIS = 2 * 60 * 60 * 1000L; // 2 gio

    private record Entry(List<ChatMessage> messages, int turnSeq, long lastAccessMillis) {
    }

    private final Map<UUID, Entry> conversations = new ConcurrentHashMap<>();

    public List<ChatMessage> history(UUID conversationId) {
        evictStale();
        Entry entry = conversations.get(conversationId);
        return entry == null ? List.of() : entry.messages();
    }

    public int nextTurnSeq(UUID conversationId) {
        Entry entry = conversations.get(conversationId);
        return entry == null ? 0 : entry.turnSeq() + 1;
    }

    public void append(UUID conversationId, int turnSeq, ChatMessage userMessage, ChatMessage assistantMessage) {
        conversations.compute(conversationId, (id, existing) -> {
            List<ChatMessage> messages = existing == null ? new ArrayList<>() : new ArrayList<>(existing.messages());
            messages.add(userMessage);
            messages.add(assistantMessage);
            while (messages.size() > MAX_MESSAGES) {
                messages.remove(0);
            }
            return new Entry(messages, turnSeq, Instant.now().toEpochMilli());
        });
    }

    private void evictStale() {
        long now = Instant.now().toEpochMilli();
        conversations.entrySet().removeIf(e -> now - e.getValue().lastAccessMillis() > STALE_AFTER_MILLIS);
    }
}
