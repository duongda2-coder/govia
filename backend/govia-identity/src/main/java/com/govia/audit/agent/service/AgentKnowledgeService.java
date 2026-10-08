package com.govia.audit.agent.service;

import com.govia.audit.agent.dto.KnowledgeHit;
import com.govia.audit.agent.llm.EmbeddingClient;
import com.govia.audit.documentlibrary.dto.AuditDocumentLibraryResponse;
import com.govia.audit.documentlibrary.service.AuditDocumentLibraryService;
import com.govia.core.attachment.AttachmentService;
import com.govia.core.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Kho tri thuc (RAG) giai doan G1: tim trong "Thu vien tai lieu" (van ban, quy dinh noi bo). CHI DOC
 * qua AuditDocumentLibraryService.list() co san - khong them cot/bang nao vao bang nghiep vu.
 *
 * <p>Chi muc (vector + text da chuan hoa) giu TRONG BO NHO theo tenant, cap nhat tang dan moi lan tim:
 * tai lieu moi/da sua (doi hash noi dung) moi duoc embed lai, tai lieu da xoa bi bo khoi chi muc.
 * Thu vien vai tram van ban nen cach nay du nhanh va khong can extension pgvector; khi khoi luong
 * lon hon (G2+: nap phat hien cac nam truoc) moi chuyen sang pgvector.
 *
 * <p>Diem = 0.7 x cosine (neu co embedding) + 0.3 x diem tu khoa; khong co embedding thi chi dung
 * diem tu khoa (unigram + bigram am tiet, trong so IDF de tu chung chung nhu "quy dinh" it gia tri).
 */
@Service
public class AgentKnowledgeService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;
    private static final int EMBED_BATCH = 32;
    private static final int EXCERPT_LENGTH = 600;
    private static final double MIN_KEYWORD_SCORE = 0.25;
    private static final double MIN_COSINE = 0.35;
    private static final long EMBED_RETRY_AFTER_MILLIS = 5 * 60 * 1000L;

    private static final int FILE_TEXT_MAX = 20_000;
    private static final int EMBED_TEXT_MAX = 4_000;

    /** fileText: noi dung chu cac file dinh kem (chi co khi bat doc file - xem AgentFileService), rong neu khong. */
    private record IndexedDoc(AuditDocumentLibraryResponse doc, int hash, String normalizedText, String fileText, float[] vector) {
    }

    private final AuditDocumentLibraryService documentLibraryService;
    private final EmbeddingClient embeddingClient;
    private final AgentFileService fileService;
    private final AttachmentService attachmentService;
    private final Map<UUID, Map<UUID, IndexedDoc>> indexByTenant = new ConcurrentHashMap<>();
    private volatile long embedFailedAtMillis = 0;

    public AgentKnowledgeService(AuditDocumentLibraryService documentLibraryService, EmbeddingClient embeddingClient,
                                 AgentFileService fileService, AttachmentService attachmentService) {
        this.documentLibraryService = documentLibraryService;
        this.embeddingClient = embeddingClient;
        this.fileService = fileService;
        this.attachmentService = attachmentService;
    }

    public List<KnowledgeHit> searchDocuments(String query, Integer limit, Boolean includeExpired) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int effectiveLimit = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        List<IndexedDoc> docs = syncIndex().stream()
                .filter(d -> Boolean.TRUE.equals(includeExpired) || !d.doc().expired())
                .toList();
        if (docs.isEmpty()) {
            return List.of();
        }

        float[] queryVector = embedQuery(query);
        List<String> units = queryUnits(query);
        Map<String, Double> idf = idf(units, docs);
        double idfTotal = idf.values().stream().mapToDouble(Double::doubleValue).sum();

        record Scored(IndexedDoc doc, double score, String mode) {
        }
        List<Scored> scored = new ArrayList<>();
        for (IndexedDoc d : docs) {
            double keyword = idfTotal == 0 ? 0 : units.stream()
                    .filter(u -> AgentText.containsPhrase(d.normalizedText(), u))
                    .mapToDouble(u -> idf.getOrDefault(u, 0.0)).sum() / idfTotal;
            boolean semantic = queryVector != null && d.vector() != null && d.vector().length == queryVector.length;
            double cosine = semantic ? cosine(queryVector, d.vector()) : 0;
            if (keyword < MIN_KEYWORD_SCORE && cosine < MIN_COSINE) {
                continue;
            }
            double score = semantic ? 0.7 * cosine + 0.3 * keyword : keyword;
            scored.add(new Scored(d, score, semantic ? "semantic" : "keyword"));
        }
        return scored.stream()
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(effectiveLimit)
                .map(s -> toHit(s.doc(), s.score(), s.mode(), units))
                .toList();
    }

    private List<IndexedDoc> syncIndex() {
        UUID tenantId = TenantContext.getTenantId();
        Map<UUID, IndexedDoc> index = indexByTenant.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>());
        List<AuditDocumentLibraryResponse> current = documentLibraryService.list();
        Set<UUID> currentIds = current.stream().map(AuditDocumentLibraryResponse::id).collect(Collectors.toSet());
        index.keySet().removeIf(id -> !currentIds.contains(id));

        // Bat doc file: dem so file moi van ban 1 lan (1 truy van) - so file doi thi doc lai file cua van ban do
        Map<UUID, Long> fileCounts = fileService.enabled() && !currentIds.isEmpty()
                ? attachmentService.countByEntity(AgentFileService.DOCUMENT_LIBRARY, List.copyOf(currentIds)) : Map.of();
        List<IndexedDoc> needVector = new ArrayList<>();
        for (AuditDocumentLibraryResponse doc : current) {
            String text = indexText(doc);
            long files = fileCounts.getOrDefault(doc.id(), 0L);
            int hash = (text + "#files=" + files + "#" + fileService.enabled()).hashCode();
            IndexedDoc existing = index.get(doc.id());
            if (existing == null || existing.hash() != hash) {
                String fileText = files > 0 ? fileService.documentLibraryText(doc.id(), FILE_TEXT_MAX).orElse("") : "";
                IndexedDoc fresh = new IndexedDoc(doc, hash, AgentText.normalize(text + "\n" + fileText), fileText, null);
                index.put(doc.id(), fresh);
                needVector.add(fresh);
            } else if (existing.vector() == null) {
                needVector.add(existing);
            }
        }
        embedDocs(index, needVector);
        return current.stream().map(d -> index.get(d.id())).filter(Objects::nonNull).toList();
    }

    private void embedDocs(Map<UUID, IndexedDoc> index, List<IndexedDoc> docs) {
        if (docs.isEmpty() || !embeddingAllowedNow()) {
            return;
        }
        for (int start = 0; start < docs.size(); start += EMBED_BATCH) {
            List<IndexedDoc> batch = docs.subList(start, Math.min(start + EMBED_BATCH, docs.size()));
            Optional<List<float[]>> vectors = embeddingClient.embed(batch.stream().map(AgentKnowledgeService::embedText).toList());
            if (vectors.isEmpty()) {
                embedFailedAtMillis = System.currentTimeMillis();
                return;
            }
            for (int i = 0; i < batch.size(); i++) {
                IndexedDoc d = batch.get(i);
                index.put(d.doc().id(), new IndexedDoc(d.doc(), d.hash(), d.normalizedText(), d.fileText(), vectors.get().get(i)));
            }
        }
    }

    private float[] embedQuery(String query) {
        if (!embeddingAllowedNow()) {
            return null;
        }
        Optional<List<float[]>> vectors = embeddingClient.embed(List.of(query));
        if (vectors.isEmpty()) {
            embedFailedAtMillis = System.currentTimeMillis();
            return null;
        }
        return vectors.get().get(0);
    }

    /** Server embedding vua loi thi tam ngung goi 5 phut - tranh moi lan tim deu cho het timeout. */
    private boolean embeddingAllowedNow() {
        return embeddingClient.isConfigured() && System.currentTimeMillis() - embedFailedAtMillis > EMBED_RETRY_AFTER_MILLIS;
    }

    private static List<String> queryUnits(String query) {
        List<String> tokens = AgentText.tokens(query).stream().filter(t -> t.length() >= 2).toList();
        Set<String> units = new LinkedHashSet<>(tokens);
        for (int i = 0; i + 1 < tokens.size(); i++) {
            units.add(tokens.get(i) + " " + tokens.get(i + 1));
        }
        return new ArrayList<>(units);
    }

    private static Map<String, Double> idf(List<String> units, List<IndexedDoc> docs) {
        int n = docs.size();
        return units.stream().collect(Collectors.toMap(u -> u, u -> {
            long df = docs.stream().filter(d -> AgentText.containsPhrase(d.normalizedText(), u)).count();
            return df == 0 ? 0.0 : Math.log(1.0 + (double) n / df);
        }, (a, b) -> a));
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return na == 0 || nb == 0 ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static String indexText(AuditDocumentLibraryResponse d) {
        return Stream.of(d.documentNumber(), d.documentName(), d.topic(), d.businessActivity(), d.issuerPositionName(),
                        d.legalBasis(), d.replacedDocument(), d.amendedDocument(), d.content())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private static KnowledgeHit toHit(IndexedDoc indexed, double score, String mode, List<String> units) {
        AuditDocumentLibraryResponse d = indexed.doc();
        String content = d.content() == null ? null
                : d.content().length() > EXCERPT_LENGTH ? d.content().substring(0, EXCERPT_LENGTH) + "..." : d.content();
        return new KnowledgeHit(d.id(), d.documentNumber(), d.documentName(), d.topic(), d.businessActivity(),
                d.issueDate(), d.effectiveDate(), d.expired(), d.expiryDate(), d.legalBasis(), content,
                BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP), mode, bestFileExcerpt(indexed.fileText(), units));
    }

    private static String embedText(IndexedDoc d) {
        String text = indexText(d.doc()) + (d.fileText() == null || d.fileText().isEmpty() ? "" : "\n" + d.fileText());
        return text.length() > EMBED_TEXT_MAX ? text.substring(0, EMBED_TEXT_MAX) : text;
    }

    /** Doan ~600 ky tu trong noi dung file chua nhieu tu khoa cau hoi nhat (tung doan van) - de AI trich dan
     * dung cho trong quy dinh, khong chi ten van ban. Rong neu van ban khong co file hoac dang tat doc file. */
    private static String bestFileExcerpt(String fileText, List<String> units) {
        if (fileText == null || fileText.isBlank()) {
            return null;
        }
        String best = null;
        long bestHits = 0;
        for (String paragraph : fileText.split("\n")) {
            String normalized = AgentText.normalize(paragraph);
            long hits = units.stream().filter(u -> AgentText.containsPhrase(normalized, u)).count();
            if (hits > bestHits) {
                bestHits = hits;
                best = paragraph.trim();
            }
        }
        if (best == null) {
            return null;
        }
        return best.length() > EXCERPT_LENGTH ? best.substring(0, EXCERPT_LENGTH) + "..." : best;
    }
}
