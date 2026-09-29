package com.govia.audit.dgcl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Bo tieu chi co dinh cua 3 phu luc, sinh tu sheet PL 01A/PL 01B/PL 01F cua DGCL_CN.xlsx vao
 * templates/audit/dgcl-criteria.json. itemKey (A001, B001, F001...) la khoa noi voi dong du lieu da luu -
 * neu sua bo tieu chi thi CHI them key moi, khong danh so lai key cu. */
@Component
public class DgclCriteriaCatalog {

    static final String RESOURCE = "templates/audit/dgcl-criteria.json";

    /** PL01B: segment = cot "Nghiệp vụ" cua file pl01b (test 29.9) - CHUNG = dung chung moi mang, con lai la ma mang (CE, LN, DP...).
     * tick = cot "tick" cua file 01A/pl01b: tieu chi NSD can cham, phieu moi tu tich san NDTH + Tuân thủ.
     * PL01A/PL01B: kind TOTAL_COUNT/RATIO/SCORE = dong IV/V/VI, DISRUPTION = dong "Xảy ra tình trạng gián đoạn..." (chi PL01A).
     * PL01F: kind/rate/flag mo ta vai tro cua dong trong cong thuc (xem {@link DgclScoring}). */
    public record Item(String key, String stt, String content, Boolean header, String segment, String kind, Double rate, String flag, Boolean tick) {
        public static final String COMMON_SEGMENT = "CHUNG";

        public boolean isHeader() {
            return Boolean.TRUE.equals(header);
        }

        public boolean isTick() {
            return Boolean.TRUE.equals(tick);
        }

        /** Dong tong hop IV/V/VI cua PL01A/PL01B: chi hien thi so lieu tinh ra, khong nhap. */
        public boolean isFooter() {
            return "TOTAL_COUNT".equals(kind) || "RATIO".equals(kind) || "SCORE".equals(kind);
        }
    }

    private final Map<DgclAppendix, List<Item>> items = new EnumMap<>(DgclAppendix.class);

    public DgclCriteriaCatalog() {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            Map<String, List<Item>> raw = mapper.readValue(in, new TypeReference<>() {
            });
            raw.forEach((appendix, list) -> items.put(DgclAppendix.valueOf(appendix), List.copyOf(list)));
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc bo tieu chi DGCL " + RESOURCE, e);
        }
    }

    public List<Item> items(DgclAppendix appendix) {
        return items.getOrDefault(appendix, List.of());
    }
}
