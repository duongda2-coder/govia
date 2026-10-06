package com.govia.audit.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.govia.audit.agent.config.AgentProperties;
import com.govia.audit.agent.dto.AgentChatResponse;
import com.govia.audit.agent.dto.AgentConversationSummary;
import com.govia.audit.agent.dto.AgentMessageResponse;
import com.govia.audit.agent.dto.EvidenceRef;
import com.govia.audit.agent.entity.AgentConversation;
import com.govia.audit.agent.entity.AgentMessage;
import com.govia.audit.agent.llm.ChatMessage;
import com.govia.audit.agent.repository.AgentConversationRepository;
import com.govia.audit.agent.repository.AgentMessageRepository;
import com.govia.core.tenant.TenantContext;
import com.govia.core.web.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Luu hoi thoai vao DB (bang agent_conversation / agent_message) - thay ban luu trong bo nho cua MVP
 * (mat khi restart). Moi hoi thoai thuoc ve DUY NHAT 1 nguoi dung: nguoi khac (ke ca biet
 * conversationId) khong doc/ghi tiep duoc. Chi {@link AgentProperties#getHistoryMessages()} message
 * gan nhat duoc dua lai cho model, tranh tran context window.
 */
@Component
public class ConversationStore {

    private static final int MAX_TEXT = 4000;
    private static final int TITLE_LENGTH = 80;
    private static final int MAX_LIST = 50;

    private final AgentConversationRepository conversationRepository;
    private final AgentMessageRepository messageRepository;
    private final AgentProperties agentProperties;
    private final ObjectMapper objectMapper;

    public ConversationStore(AgentConversationRepository conversationRepository, AgentMessageRepository messageRepository,
                             AgentProperties agentProperties, ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.agentProperties = agentProperties;
        this.objectMapper = objectMapper;
    }

    /** Hoi thoai cua chinh nguoi dung, empty neu chua co; nem 403 neu conversationId la cua nguoi khac. */
    @Transactional(readOnly = true)
    public Optional<AgentConversation> find(UUID conversationKey, UUID userId) {
        Optional<AgentConversation> conversation = conversationRepository.findByTenantIdAndConversationKey(TenantContext.getTenantId(), conversationKey);
        conversation.ifPresent(c -> requireOwner(c, userId));
        return conversation;
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> history(UUID conversationKey) {
        List<AgentMessage> latest = new ArrayList<>(messageRepository.findByTenantIdAndConversationIdOrderBySeqDesc(
                TenantContext.getTenantId(), conversationKey, PageRequest.of(0, Math.max(agentProperties.getHistoryMessages(), 2))));
        java.util.Collections.reverse(latest);
        return latest.stream()
                .map(m -> m.getRole() == AgentMessage.Role.USER ? ChatMessage.user(m.getContent()) : ChatMessage.assistant(m.getContent()))
                .toList();
    }

    @Transactional
    public void append(UUID conversationKey, UUID userId, String userText, AgentChatResponse response, String pageLabel) {
        UUID tenantId = TenantContext.getTenantId();
        Instant now = Instant.now();
        AgentConversation conversation = conversationRepository.findByTenantIdAndConversationKey(tenantId, conversationKey)
                .orElseGet(() -> {
                    AgentConversation c = new AgentConversation();
                    c.setTenantId(tenantId);
                    c.setConversationKey(conversationKey);
                    c.setUserId(userId);
                    c.setTitle(truncate(userText.replaceAll("\\s+", " ").trim(), TITLE_LENGTH));
                    return c;
                });
        requireOwner(conversation, userId);

        int seq = conversation.getMessageCount();
        AgentMessage question = message(tenantId, conversationKey, ++seq, AgentMessage.Role.USER, userText, pageLabel);
        AgentMessage answer = message(tenantId, conversationKey, ++seq, AgentMessage.Role.ASSISTANT, response.answer(), pageLabel);
        answer.setAgentCode(response.agentCode());
        answer.setResponseJson(compactJson(response));
        messageRepository.save(question);
        messageRepository.save(answer);

        conversation.setMessageCount(seq);
        conversation.setLastAgentCode(response.agentCode());
        conversation.setLastMessageAt(now);
        conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public List<AgentConversationSummary> listForUser(UUID userId, Integer limit) {
        int size = limit == null || limit <= 0 ? 30 : Math.min(limit, MAX_LIST);
        return conversationRepository.findByTenantIdAndUserIdAndArchivedFalseOrderByLastMessageAtDesc(
                        TenantContext.getTenantId(), userId, PageRequest.of(0, size)).stream()
                .map(c -> new AgentConversationSummary(c.getConversationKey(), c.getTitle(), c.getLastAgentCode(),
                        c.getLastMessageAt(), c.getMessageCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgentMessageResponse> messages(UUID conversationKey, UUID userId) {
        if (find(conversationKey, userId).isEmpty()) {
            throw new BusinessException("AGENT_CONVERSATION_NOT_FOUND", "Khong tim thay cuoc tro chuyen", HttpStatus.NOT_FOUND);
        }
        return messageRepository.findByTenantIdAndConversationIdOrderBySeqAsc(TenantContext.getTenantId(), conversationKey).stream()
                .map(m -> new AgentMessageResponse(m.getId(), m.getSeq(), m.getRole().name(), m.getContent(), m.getAgentCode(),
                        parseResponse(m.getResponseJson()), m.getPageLabel(), m.getCreatedAt()))
                .toList();
    }

    /** "Xoa" khoi danh sach cua nguoi dung = an di (archived), van giu cho truy vet cung agent_tool_call_log. */
    @Transactional
    public void archive(UUID conversationKey, UUID userId) {
        AgentConversation conversation = find(conversationKey, userId).orElseThrow(() ->
                new BusinessException("AGENT_CONVERSATION_NOT_FOUND", "Khong tim thay cuoc tro chuyen", HttpStatus.NOT_FOUND));
        conversation.setArchived(true);
        conversationRepository.save(conversation);
    }

    private void requireOwner(AgentConversation conversation, UUID userId) {
        if (!conversation.getUserId().equals(userId)) {
            throw new BusinessException("AGENT_CONVERSATION_FORBIDDEN", "Cuoc tro chuyen nay khong thuoc ve ban", HttpStatus.FORBIDDEN);
        }
    }

    private AgentMessage message(UUID tenantId, UUID conversationKey, int seq, AgentMessage.Role role, String content, String pageLabel) {
        AgentMessage m = new AgentMessage();
        m.setTenantId(tenantId);
        m.setConversationId(conversationKey);
        m.setSeq(seq);
        m.setRole(role);
        m.setContent(truncate(content == null || content.isBlank() ? "(trong)" : content, MAX_TEXT));
        m.setPageLabel(truncate(pageLabel, 300));
        return m;
    }

    /**
     * JSON cua cau tra loi co cau truc, vua 4000 ky tu: thu lan luot tu nhe den nang - bo so lieu chi
     * tiet cua evidence, cat ngan tung dong, giu toi da 5 dong moi muc, bo han evidence. Khong vua nua
     * thi tra null (giao dien chi hien noi dung "answer" trong cot content).
     */
    private String compactJson(AgentChatResponse response) {
        List<UnaryOperator<AgentChatResponse>> steps = List.of(
                r -> r,
                r -> withEvidence(r, r.evidence().stream().map(e -> new EvidenceRef(e.tool(), e.args(), Map.of())).toList()),
                r -> mapLists(r, 300, Integer.MAX_VALUE),
                r -> mapLists(r, 200, 5),
                r -> withEvidence(r, List.of()));
        AgentChatResponse current = response;
        for (UnaryOperator<AgentChatResponse> step : steps) {
            current = step.apply(current);
            try {
                String json = objectMapper.writeValueAsString(current);
                if (json.length() <= MAX_TEXT) {
                    return json;
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static AgentChatResponse withEvidence(AgentChatResponse r, List<EvidenceRef> evidence) {
        return new AgentChatResponse(truncate(r.answer(), 1500), r.facts(), r.analysis(), r.recommendations(), evidence,
                r.metadata(), r.agentCode(), r.agentName());
    }

    private static AgentChatResponse mapLists(AgentChatResponse r, int maxItemLength, int maxItems) {
        UnaryOperator<List<String>> cut = list -> list.stream().limit(maxItems).map(s -> truncate(s, maxItemLength)).toList();
        return new AgentChatResponse(truncate(r.answer(), 1500), cut.apply(r.facts()), cut.apply(r.analysis()),
                cut.apply(r.recommendations()), r.evidence(), r.metadata(), r.agentCode(), r.agentName());
    }

    private AgentChatResponse parseResponse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, AgentChatResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max - 3) + "..." : value;
    }
}
