package com.govia.audit.agent.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Chuan hoa tieng Viet de so khop tu khoa: bo dau, "đ" -> "d", chu thuong, gop khoang trang - de
 * "Rủi ro", "rui ro", "RUI RO" deu khop nhau (nguoi dung go co dau lan khong dau deu phai hieu duoc).
 */
public final class AgentText {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9]+");

    private AgentText() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT).replace('đ', 'd');
        String stripped = DIACRITICS.matcher(Normalizer.normalize(lower, Normalizer.Form.NFD)).replaceAll("");
        return NON_WORD.matcher(stripped).replaceAll(" ").trim();
    }

    public static List<String> tokens(String text) {
        String normalized = normalize(text);
        return normalized.isEmpty() ? List.of() : Arrays.asList(normalized.split(" "));
    }

    /** true neu cau (da chuan hoa) chua cum tu (da chuan hoa) nhu 1 cum tu tron ven, khong cat giua tu. */
    public static boolean containsPhrase(String normalizedText, String phrase) {
        String p = normalize(phrase);
        return !p.isEmpty() && (" " + normalizedText + " ").contains(" " + p + " ");
    }
}
