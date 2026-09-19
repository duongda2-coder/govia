package com.govia.audit.khkt.khnsnam.allocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Thuat toan phan bo can bo cho cac doan kiem toan ca nam (man hinh KHNS_PB "Phan bo nhan su") -
 * thuan logic, khong phu thuoc Spring/DB de de kiem thu. Theo "Nguyen tac bo tri nhan su 1 doan
 * kiem toan" (yeu cau test18.9):
 * <ul>
 *   <li>Toi da 9 can bo / doan; doi tuong khong co linh vuc Tin dung (LN) chi bo tri toi da 4.</li>
 *   <li>Tin dung (LN): toi thieu 02 KTV, tu trung binh tro len toi thieu 03 can bo.</li>
 *   <li>HDV-PCRT (DP/AM), TCKT (GA), XDCB (FA), TTQT (TF): moi nghiep vu 01 KTV. CNTT/The/TTKQ
 *       (IT/CD/MF): co tu 2 trong 3 nghiep vu thi bo tri them 01 KTV.</li>
 *   <li>Linh vuc quy mo lon: KTV bac cao (toi thieu bac 2); trung binh/thap: tu bac 1.</li>
 *   <li>Khong bo tri KTV tai don vi minh tung lam viec / don vi co nguoi lien quan (loai cung).</li>
 *   <li>Can bang do phuc tap giua cac doan: xu ly doan phuc tap truoc, uu tien can bo it doan nhat,
 *       KTV bac cao chi dung khi can (tranh don het vao 1 doan).</li>
 * </ul>
 * Rang buoc cua mo hinh du lieu KHNS_NAM: moi can bo chi co 1 doi tuong / thang, va 1 "Chuc vu" cho
 * ca nam - nen 1 can bo da lam Truong doan thi chi lam Truong doan o moi doan (khong tron vai tro).
 */
public final class AuditKhnsPbAllocator {

    public static final String CREDIT = "LN";
    private static final Set<String> HDV_PCRT = Set.of("DP", "AM");
    private static final String TCKT = "GA";
    private static final String XDCB = "FA";
    private static final String TTQT = "TF";
    private static final Set<String> IT_CARD_TREASURY = Set.of("IT", "CD", "MF");

    static final int MAX_TEAM_SIZE = 9;
    static final int MAX_TEAM_SIZE_NON_CREDIT = 4;

    public enum ScaleLevel { LOW, MEDIUM, LARGE }

    /** 1 doi tuong kiem toan can bo tri doan: kiem toan vao cac thang {@code months}. */
    public record TeamObject(String code, String name, Set<Integer> months, Set<String> segmentCodes,
                              ScaleLevel creditLevel, ScaleLevel fundingLevel) {
    }

    /** grade: bac KTV (1..3); capableSegments: ma nghiep vu (BUSINESS_SEGMENT) can bo dam nhan duoc. */
    public record Staff(UUID id, String code, String name, int grade, Set<String> capableSegments, boolean leadCapable,
                         String ownSegmentCode, Set<String> blockedObjectCodes, Set<String> rotatedObjectCodes) {
    }

    public static final class StaffAssignment {
        private boolean lead;
        private boolean member;
        private final Map<Integer, String> monthToObject = new TreeMap<>();
        private final Set<String> objectCodes = new LinkedHashSet<>();

        public boolean isLead() {
            return lead;
        }

        public Map<Integer, String> getMonthToObject() {
            return monthToObject;
        }

        public Set<String> getObjectCodes() {
            return objectCodes;
        }
    }

    public record Result(Map<UUID, StaffAssignment> assignments, List<String> warnings, int objectCount, int objectsFullyStaffed) {
    }

    private record Slot(String label, Set<String> segments, int minGrade) {
    }

    private record TeamPlan(TeamObject object, List<Slot> slots, Set<String> bonusSegments, int maxSize) {
    }

    public Result allocate(List<TeamObject> objects, List<Staff> staff) {
        Map<UUID, StaffAssignment> assignments = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        List<TeamPlan> plans = objects.stream().map(this::plan)
                .sorted(Comparator.comparingInt((TeamPlan p) -> p.object().months().size()).reversed()
                        .thenComparing(Comparator.comparingInt((TeamPlan p) -> p.slots().size()).reversed())
                        .thenComparing(p -> p.object().code()))
                .toList();

        int fullyStaffed = 0;
        for (TeamPlan plan : plans) {
            if (staffTeam(plan, staff, assignments, warnings)) {
                fullyStaffed++;
            }
        }
        return new Result(assignments, warnings, plans.size(), fullyStaffed);
    }

    private TeamPlan plan(TeamObject object) {
        Set<String> segments = object.segmentCodes();
        boolean hasCredit = segments.contains(CREDIT);
        List<Slot> slots = new ArrayList<>();

        if (hasCredit) {
            int count = object.creditLevel() == ScaleLevel.LOW ? 2 : 3;
            int minGrade = object.creditLevel() == ScaleLevel.LARGE ? 2 : 1;
            for (int i = 0; i < count; i++) {
                slots.add(new Slot("Tín dụng", Set.of(CREDIT), minGrade));
            }
        }
        Set<String> hdvPcrt = intersection(segments, HDV_PCRT);
        if (!hdvPcrt.isEmpty()) {
            int minGrade = segments.contains("DP") && object.fundingLevel() == ScaleLevel.LARGE ? 2 : 1;
            slots.add(new Slot("HĐV-PCRT", hdvPcrt, minGrade));
        }
        if (segments.contains(TCKT)) {
            slots.add(new Slot("TCKT", Set.of(TCKT), 1));
        }
        if (segments.contains(XDCB)) {
            slots.add(new Slot("XDCB", Set.of(XDCB), 1));
        }
        if (segments.contains(TTQT)) {
            slots.add(new Slot("TTQT", Set.of(TTQT), 1));
        }
        Set<String> itCardTreasury = intersection(segments, IT_CARD_TREASURY);
        if (itCardTreasury.size() >= 2) {
            slots.add(new Slot("CNTT/Thẻ/TTKQ", itCardTreasury, 1));
        }

        int maxSize = hasCredit ? MAX_TEAM_SIZE : MAX_TEAM_SIZE_NON_CREDIT;
        while (slots.size() > maxSize) {
            slots.remove(slots.size() - 1);
        }

        Set<String> covered = new HashSet<>();
        slots.forEach(s -> covered.addAll(s.segments()));
        Set<String> bonus = new HashSet<>(segments);
        bonus.removeAll(covered);
        return new TeamPlan(object, slots, bonus, maxSize);
    }

    /** @return true neu doan du dinh bien (du Truong doan va du cac vi tri). */
    private boolean staffTeam(TeamPlan plan, List<Staff> staff, Map<UUID, StaffAssignment> assignments, List<String> warnings) {
        List<Slot> open = new ArrayList<>(plan.slots());
        boolean complete = true;

        // 1. Truong doan - uu tien nguoi vua lam Truong doan vua dam nhan duoc 1 vi tri (khong ton them cho)
        Staff lead = null;
        Slot leadSlot = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Staff s : staff) {
            StaffAssignment current = assignments.get(s.id());
            if (!s.leadCapable() || (current != null && current.member) || !available(s, plan.object(), current)) {
                continue;
            }
            Slot slot = bestSlotFor(s, open);
            double score = (slot != null ? 100 : 0) + score(s, plan, current, slot, false);
            if (score > bestScore || (score == bestScore && lead != null && s.code().compareTo(lead.code()) < 0)) {
                bestScore = score;
                lead = s;
                leadSlot = slot;
            }
        }
        if (lead == null) {
            warnings.add(describe(plan.object()) + ": chưa tìm được Trưởng đoàn phù hợp (còn trống trong các tháng kiểm toán).");
            complete = false;
        } else if (leadSlot == null && plan.slots().size() + 1 > plan.maxSize()) {
            warnings.add(describe(plan.object()) + ": không bố trí thêm Trưởng đoàn vì đoàn đã đủ số lượng tối đa (" + plan.maxSize() + " cán bộ).");
            complete = false;
            lead = null;
        }
        if (lead != null) {
            assign(assignments, lead, plan.object(), true);
            if (leadSlot != null) {
                open.remove(leadSlot);
            }
        }

        // 2. Cac vi tri con lai - vi tri it nguoi dap ung nhat xu ly truoc
        while (!open.isEmpty()) {
            Slot target = null;
            List<Staff> targetCandidates = null;
            for (Slot slot : open) {
                List<Staff> candidates = candidatesFor(slot, plan.object(), staff, assignments);
                if (target == null || candidates.size() < targetCandidates.size()
                        || (candidates.size() == targetCandidates.size() && slot.minGrade() > target.minGrade())) {
                    target = slot;
                    targetCandidates = candidates;
                }
            }
            open.remove(target);
            Staff picked = null;
            double pickedScore = Double.NEGATIVE_INFINITY;
            for (Staff s : targetCandidates) {
                double score = score(s, plan, assignments.get(s.id()), target, true);
                if (score > pickedScore || (score == pickedScore && s.code().compareTo(picked.code()) < 0)) {
                    picked = s;
                    pickedScore = score;
                }
            }
            if (picked == null) {
                warnings.add(describe(plan.object()) + ": thiếu 01 cán bộ cho nghiệp vụ " + target.label()
                        + (target.minGrade() > 1 ? " (yêu cầu KTV từ bậc " + target.minGrade() + ")" : "") + ".");
                complete = false;
            } else {
                assign(assignments, picked, plan.object(), false);
            }
        }
        return complete;
    }

    private List<Staff> candidatesFor(Slot slot, TeamObject object, List<Staff> staff, Map<UUID, StaffAssignment> assignments) {
        List<Staff> result = new ArrayList<>();
        for (Staff s : staff) {
            StaffAssignment current = assignments.get(s.id());
            if ((current != null && current.lead) || !available(s, object, current) || !fits(s, slot)) {
                continue;
            }
            result.add(s);
        }
        return result;
    }

    private boolean fits(Staff s, Slot slot) {
        return s.grade() >= slot.minGrade() && !intersection(s.capableSegments(), slot.segments()).isEmpty();
    }

    /** Vi tri kho nhat (bac yeu cau cao nhat) ma can bo dam nhan duoc, hoac null. */
    private Slot bestSlotFor(Staff s, List<Slot> open) {
        Slot best = null;
        for (Slot slot : open) {
            if (fits(s, slot) && (best == null || slot.minGrade() > best.minGrade())) {
                best = slot;
            }
        }
        return best;
    }

    private boolean available(Staff s, TeamObject object, StaffAssignment current) {
        if (s.blockedObjectCodes().contains(object.code())) {
            return false;
        }
        if (current == null) {
            return true;
        }
        for (Integer month : object.months()) {
            if (current.monthToObject.containsKey(month)) {
                return false;
            }
        }
        return true;
    }

    /** Diem cang cao cang duoc chon: it thang da phan bo, khong phai doi tuong vua kiem toan nam
     * truoc, dung linh vuc du kien, KTV bac thap nhat du dap ung (danh bac cao cho doan can),
     * phu them nghiep vu khong co vi tri rieng, va tranh dung nguoi co the lam Truong doan lam thanh vien. */
    private double score(Staff s, TeamPlan plan, StaffAssignment current, Slot slot, boolean asMember) {
        double score = -3.0 * (current == null ? 0 : current.monthToObject.size());
        if (s.rotatedObjectCodes().contains(plan.object().code())) {
            score -= 8;
        }
        if (slot != null) {
            if (s.ownSegmentCode() != null && slot.segments().contains(s.ownSegmentCode())) {
                score += 6;
            }
            score -= 2.0 * (s.grade() - slot.minGrade());
        }
        score += 2.0 * intersection(s.capableSegments(), plan.bonusSegments()).size();
        if (asMember && s.leadCapable()) {
            score -= 5;
        }
        return score;
    }

    private void assign(Map<UUID, StaffAssignment> assignments, Staff s, TeamObject object, boolean asLead) {
        StaffAssignment a = assignments.computeIfAbsent(s.id(), k -> new StaffAssignment());
        if (asLead) {
            a.lead = true;
        } else {
            a.member = true;
        }
        for (Integer month : object.months()) {
            a.monthToObject.put(month, object.code());
        }
        a.objectCodes.add(object.code());
    }

    private String describe(TeamObject object) {
        String months = object.months().stream().sorted().map(String::valueOf).reduce((a, b) -> a + ", " + b).orElse("");
        return "Đối tượng " + object.code() + " - " + object.name() + " (tháng " + months + ")";
    }

    private Set<String> intersection(Set<String> a, Set<String> b) {
        Set<String> result = new HashSet<>(a);
        result.retainAll(b);
        return result;
    }
}
