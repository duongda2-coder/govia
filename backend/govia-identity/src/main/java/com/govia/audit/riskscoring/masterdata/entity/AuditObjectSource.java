package com.govia.audit.riskscoring.masterdata.entity;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Danh muc "Doi tuong kiem toan" cu the ma 1 AuditObjectCategory ("Loai doi tuong kiem toan") tro
 * toi khi tra cuu "Ma doi tuong KT" (vd o man hinh Cham diem rui ro khac, sheet ZTC_CDRR_KHAC).
 * Truoc day quan he nay bi hard-code theo dung 3 MA co dinh (HO/CTC/KTQT), moi ma khac deu roi vao
 * PROJECT mac dinh - sai ngay khi NSD tao 1 category moi (vd "QT") ma khong dat dung ma "KTQT". Nay
 * chuyen thanh 1 truong tuong minh, nhung NSD KHONG bat buoc phai tu chon: guess() tu doan dung
 * theo ten/ma category (xem AuditObjectCategoryService) - NSD chi can ghi de neu doan sai.
 */
public enum AuditObjectSource {
    UNIT,       // ZTC_DTKT1 - AuditObjectUnit (HO/Giam sat CC/Chi nhanh)
    SUBSIDIARY, // ZTC_DTKT2 - AuditObjectSubsidiary (Cong ty con)
    PROCESS,    // ZTC_DTKT4 - AuditObjectProcess (Quy trinh)
    PROJECT;    // ZTC_DTKT3 - AuditObjectProject (Du an/DVTN) - mac dinh khi khong doan duoc gi khac

    /** Doan objectSource tu ma/ten category (bo dau, khong phan biet hoa/thuong) - dung lam gia tri
     * mac dinh khi NSD khong tu chon (form NSD luon gui gia tri tuong minh nen day chu yeu ap dung
     * cho import Excel/goi API truc tiep), va de tu sua lai cac category cu bi mac dinh sai truoc
     * khi co truong nay (xem migration 040). */
    public static AuditObjectSource guess(String code, String name) {
        String normalizedCode = stripDiacritics((code == null ? "" : code).trim().toLowerCase());
        String text = stripDiacritics(((code == null ? "" : code) + " " + (name == null ? "" : name)).toLowerCase());
        if (text.contains("quy trinh")) {
            return PROCESS;
        }
        if (text.contains("cong ty con")) {
            return SUBSIDIARY;
        }
        // "ho" chi khop khi la CA MA (khong xet trong ten, vi nhieu tu tieng Viet khac - "ho so",
        // "ho tro"... - cung bi rut gon ve "ho" sau khi bo dau, de gay nham lan).
        if (normalizedCode.equals("ho") || text.contains("don vi") || text.contains("hoi so") || text.contains("tru so")
                || text.contains("chi nhanh")) {
            return UNIT;
        }
        return PROJECT;
    }

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static String stripDiacritics(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }
}
