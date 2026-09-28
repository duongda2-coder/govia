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

    /** PL01B: segment = ma mang nghiep vu (CE, LN...) suy ra tu muc "Hoạt động ..." chua dong do.
     * PL01F: kind/rate/flag mo ta vai tro cua dong trong cong thuc (xem {@link DgclScoring}). */
    public record Item(String key, String stt, String content, Boolean header, String segment, String kind, Double rate, String flag) {
        public boolean isHeader() {
            return Boolean.TRUE.equals(header);
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
