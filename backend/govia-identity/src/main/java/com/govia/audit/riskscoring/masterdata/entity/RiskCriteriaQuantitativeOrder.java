package com.govia.audit.riskscoring.masterdata.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thu tu hien thi CHUAN cho chi tieu dinh luong - lay dung thu tu cot trong dong header (dong 5/12)
 * cua sheet DL_Nhaptructiep, file "2. Cham diem (2).xlsx": sau STT/Nam/Ma CN/Ten CN/Thoi diem cham
 * la lan luot cac nhom TDQM, TDCL, TDAT, DPQM, DPAT, DPHQ, CEAT, THKH, KDNH, FA, CNTT, CARD. Danh
 * muc chi tieu (RiskCriteriaQuantitative) khong co cot thu tu rieng nen KHONG the ORDER BY o DB -
 * sap xep lai trong bo nho theo danh sach ma tham chieu nay. Vai ma trong tai lieu goc co tien to
 * "Z" (ten truong ky thuat SAP, vd "ZCARD01") nhung ma nghiep vu thuc te dang dung khong co tien to
 * nay (vd "CARD01") - so khop sau khi bo tien to "Z" o dau.
 */
public final class RiskCriteriaQuantitativeOrder {

    private static final List<String> CANONICAL_CODES = List.of(
            "TDQM01", "TDQM02", "TDQM03", "TDQM04", "TDQM05", "TDQM06", "TDQM07", "TDQM08", "TDQM09", "TDQM010",
            "TDCL01", "TDCL02", "TDCL03", "TDCL04", "TDCL05", "TDCL06", "TDCL07", "TDCL08", "TDCL09", "TDCL10",
            "TDCL11", "TDCL12", "TDCL13", "TDCL15",
            "TDAT01", "TDAT02", "TDAT03", "TDAT04", "TDAT05", "TDAT06", "TDAT07", "TDAT08", "TDAT09", "TDAT010",
            "TDAT011", "TDAT012",
            "DPQM01", "DPQM02", "DPQM03", "DPQM04", "DPQM05", "DPQM06",
            "DPAT01", "DPAT02", "DPAT03", "DPAT04", "DPAT05", "DPAT06", "DPAT07", "DPAT08",
            "DPHQ01", "DPHQ02", "DPHQ03", "DPHQ04", "DPHQ05", "DPHQ06",
            "CEAT01", "CEAT02", "CEAT03", "CEAT04", "CEAT05", "CEAT06", "CEAT07", "CEAT08", "CEAT09",
            "THKH01", "THKH02", "THKH03", "THKH04", "THKH05", "THKH06", "THKH07", "THKH08", "THKH09",
            "KDNH01", "KDNH02", "KDNH03", "KDNH04", "KDNH05",
            "FA01", "FA02", "FA03", "FA04", "FA05",
            "CNTT01", "CNTT02", "CNTT03", "CNTT04", "CNTT05", "CNTT06",
            "CARD01", "CARD02", "CARD03", "CARD04"
    );

    /** So khop theo (tien to chu, gia tri so) thay vi nguyen van chuoi - vai ma trong tai lieu goc
     * dung 3 chu so cho muc thu 10 tro len (vd "TDAT010") trong khi ma nghiep vu dang dung tren he
     * thong lai la 2 chu so (vd "TDAT10") - can quy ve cung 1 gia tri so (10) de khop dung, neu
     * khong "TDAT10" se khong khop chuoi "TDAT010" va bi xep lac xuong cuoi danh sach. */
    private static final Pattern CODE_PATTERN = Pattern.compile("^([A-Z]+)0*(\\d+)$");

    private record CanonicalKey(String prefix, int number) {
    }

    private static final Map<CanonicalKey, Integer> INDEX_BY_KEY = new HashMap<>();

    static {
        for (int i = 0; i < CANONICAL_CODES.size(); i++) {
            CanonicalKey key = parseKey(CANONICAL_CODES.get(i));
            if (key != null) {
                INDEX_BY_KEY.putIfAbsent(key, i);
            }
        }
    }

    private RiskCriteriaQuantitativeOrder() {
    }

    /** Sap xep lai danh sach theo dung thu tu FS - ma nao khong nam trong danh sach chuan (chi tieu
     * phat sinh sau nay, chua co trong tai lieu goc) thi xep sau cung, theo alphabet. */
    public static <T> List<T> sortByFsOrder(List<T> items, Function<T, String> codeExtractor) {
        List<T> sorted = new ArrayList<>(items);
        sorted.sort(Comparator
                .<T>comparingInt(item -> rank(codeExtractor.apply(item)))
                .thenComparing(item -> codeExtractor.apply(item), String.CASE_INSENSITIVE_ORDER));
        return sorted;
    }

    private static int rank(String code) {
        CanonicalKey key = parseKey(code);
        if (key == null) {
            return Integer.MAX_VALUE;
        }
        Integer idx = INDEX_BY_KEY.get(key);
        return idx != null ? idx : Integer.MAX_VALUE;
    }

    private static CanonicalKey parseKey(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim().toUpperCase();
        if (normalized.startsWith("Z") && normalized.length() > 1) {
            normalized = normalized.substring(1);
        }
        Matcher m = CODE_PATTERN.matcher(normalized);
        if (!m.matches()) {
            return null;
        }
        return new CanonicalKey(m.group(1), Integer.parseInt(m.group(2)));
    }
}
