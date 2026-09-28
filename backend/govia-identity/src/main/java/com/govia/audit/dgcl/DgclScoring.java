package com.govia.audit.dgcl;

import com.govia.audit.dgcl.DgclCriteriaCatalog.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cong thuc cham diem cua 3 phu luc, dich tu cac o cong thuc trong DGCL_CN.xlsx:
 * <ul>
 *   <li>PL01A/PL01B (sheet logic dong 72-73): ty le = so dong "Tuân thủ" / so dong "NDTH", diem = ty le x 100
 *       (vd 91% -> 91 diem). Chi dem "Tuân thủ" tren dong co NDTH.</li>
 *   <li>PL01F (sheet logic dong 74, "tu dong B23 -> B38"): B1 = ty le PL01A x diem toi da I, B2 = ty le PL01B x diem toi da II,
 *       B3 = max(0, diem toi da III - tong diem tru chat luong) voi diem tru moi dong = so loi x ty le x diem toi da III;
 *       tong (dong 23) = B1x10% + B2x20% + B3x70%; diem cong/tru (dong IV/V) = tong ty le da tich x diem toi da dong 23;
 *       dong VI = tong + cong - tru; xep loai dong B33.</li>
 * </ul>
 * Xep loai thanh vien dung cong thuc cot F/J cua sheet PL4B1 (Chất lượng xuất sắc/tốt/khá/trung bình/không đạt yêu cầu),
 * xep loai ca doan dung cong thuc o G33 cua sheet PL 01F (Hoàn thành xuất sắc/tốt/khá/nhiệm vụ/Không hoàn thành).
 */
public final class DgclScoring {

    static final double DEFAULT_MAX_SCORE = 100d;

    private DgclScoring() {
    }

    /** Gia tri nhap tay cua 1 dong (khong phu thuoc entity de test don gian). */
    public record LineValues(boolean required, boolean compliant, boolean nonCompliant, boolean checked, Integer violationCount, Double maxScore) {
        static final LineValues EMPTY = new LineValues(false, false, false, false, null, null);
    }

    public record ComplianceResult(int requiredCount, int compliantCount, int nonCompliantCount, Double ratio, Double score) {
    }

    /** Gia tri tinh ra tren 1 dong PL01F - cot "Điểm tối đa" (C), "Điểm trừ" (E), "Điểm" (F), "Tỷ lệ" (G). */
    public record Calc(Double max, Double deduction, Double points, Double ratio) {
    }

    public record QualityResult(Map<String, Calc> calcByKey, double weightedMax, double weightedTotal, double bonusPoints,
                                double penaltyPoints, double grandTotal, double ratio, String teamClassification) {
    }

    public static ComplianceResult compliance(List<Item> items, Map<String, LineValues> values) {
        int required = 0;
        int compliant = 0;
        int nonCompliant = 0;
        for (Item item : items) {
            if (item.isHeader()) {
                continue;
            }
            LineValues v = values.getOrDefault(item.key(), LineValues.EMPTY);
            if (!v.required()) {
                continue;
            }
            required++;
            if (v.compliant()) {
                compliant++;
            } else if (v.nonCompliant()) {
                nonCompliant++;
            }
        }
        Double ratio = required == 0 ? null : (double) compliant / required;
        return new ComplianceResult(required, compliant, nonCompliant, ratio, ratio == null ? null : ratio * 100);
    }

    /** ratioA/ratioB = ty le cua PL01A/PL01B cung doi tuong (null = chua danh gia, tinh nhu 0). */
    public static QualityResult quality(List<Item> items, Map<String, LineValues> values, Double ratioA, Double ratioB) {
        Map<String, Calc> calc = new HashMap<>();
        double maxA = DEFAULT_MAX_SCORE;
        double maxB = DEFAULT_MAX_SCORE;
        double maxQ = DEFAULT_MAX_SCORE;
        for (Item item : items) {
            Double custom = values.getOrDefault(item.key(), LineValues.EMPTY).maxScore();
            if (custom == null) {
                continue;
            }
            switch (kind(item)) {
                case "SCORE_A" -> maxA = custom;
                case "SCORE_B" -> maxB = custom;
                case "SCORE_QUALITY" -> maxQ = custom;
                default -> {
                }
            }
        }

        // Diem tru chat luong theo tung nhom (1)/(2)/(3); flag BLOCKS_GOOD/BLOCKS_FAIR = o E19/E20 dung trong xep loai.
        double qualityDeduction = 0;
        double blocksGood = 0;
        double blocksFair = 0;
        String currentGroup = null;
        Map<String, Double> groupDeduction = new HashMap<>();
        for (Item item : items) {
            String kind = kind(item);
            if ("DEDUCTION_GROUP".equals(kind)) {
                currentGroup = item.key();
                groupDeduction.put(currentGroup, 0d);
            } else if ("DEDUCTION".equals(kind)) {
                Integer count = values.getOrDefault(item.key(), LineValues.EMPTY).violationCount();
                double deduction = count == null || count <= 0 ? 0 : count * rate(item) * maxQ;
                calc.put(item.key(), new Calc(null, deduction, null, null));
                qualityDeduction += deduction;
                if (currentGroup != null) {
                    groupDeduction.merge(currentGroup, deduction, Double::sum);
                }
                if ("BLOCKS_GOOD".equals(item.flag())) {
                    blocksGood += deduction;
                }
                if ("BLOCKS_FAIR".equals(item.flag())) {
                    blocksFair += deduction;
                }
            }
        }
        groupDeduction.forEach((key, sum) -> calc.put(key, new Calc(null, sum, null, null)));

        double pointsA = ratioA == null ? 0 : ratioA * maxA;
        double pointsB = ratioB == null ? 0 : ratioB * maxB;
        double pointsQ = Math.max(0, maxQ - qualityDeduction);
        double weightA = 0.1;
        double weightB = 0.2;
        double weightQ = 0.7;
        for (Item item : items) {
            switch (kind(item)) {
                case "SCORE_A" -> {
                    weightA = rate(item);
                    calc.put(item.key(), new Calc(maxA, null, pointsA, ratioOf(pointsA, maxA)));
                }
                case "SCORE_B" -> {
                    weightB = rate(item);
                    calc.put(item.key(), new Calc(maxB, null, pointsB, ratioOf(pointsB, maxB)));
                }
                case "SCORE_QUALITY" -> {
                    weightQ = rate(item);
                    calc.put(item.key(), new Calc(maxQ, qualityDeduction, pointsQ, ratioOf(pointsQ, maxQ)));
                }
                default -> {
                }
            }
        }
        double weightedMax = maxA * weightA + maxB * weightB + maxQ * weightQ;
        double weightedTotal = pointsA * weightA + pointsB * weightB + pointsQ * weightQ;

        double bonusRate = 0;
        double penaltyRate = 0;
        for (Item item : items) {
            String kind = kind(item);
            if (!"BONUS".equals(kind) && !"PENALTY".equals(kind)) {
                continue;
            }
            double r = values.getOrDefault(item.key(), LineValues.EMPTY).checked() ? rate(item) : 0;
            if ("BONUS".equals(kind)) {
                bonusRate += r;
                calc.put(item.key(), new Calc(null, null, r * weightedMax, r));
            } else {
                penaltyRate += r;
                calc.put(item.key(), new Calc(null, r * weightedMax, null, r));
            }
        }
        double bonusPoints = bonusRate * weightedMax;
        double penaltyPoints = penaltyRate * weightedMax;
        double grandTotal = weightedTotal + bonusPoints - penaltyPoints;
        double ratio = ratioOf(grandTotal, weightedMax);

        for (Item item : items) {
            switch (kind(item)) {
                case "WEIGHTED_TOTAL" -> calc.put(item.key(), new Calc(weightedMax, null, weightedTotal, ratioOf(weightedTotal, weightedMax)));
                case "BONUS_GROUP" -> calc.put(item.key(), new Calc(null, null, bonusPoints, bonusRate));
                case "PENALTY_GROUP" -> calc.put(item.key(), new Calc(null, penaltyPoints, null, penaltyRate));
                case "GRAND_TOTAL" -> calc.put(item.key(), new Calc(weightedMax, null, grandTotal, ratio));
                default -> {
                }
            }
        }

        String teamClassification = teamClassification(ratio, bonusPoints, penaltyPoints, blocksGood, blocksFair);
        return new QualityResult(calc, weightedMax, weightedTotal, bonusPoints, penaltyPoints, grandTotal, ratio, teamClassification);
    }

    /** PL 01F!G33. */
    static String teamClassification(double ratio, double bonus, double penalty, double blocksGood, double blocksFair) {
        if (ratio >= 1 && bonus > 0 && penalty == 0) {
            return "Hoàn thành xuất sắc";
        }
        if (ratio >= 0.9 && blocksGood == 0 && blocksFair == 0) {
            return "Hoàn thành tốt nhiệm vụ";
        }
        if (ratio >= 0.8 && blocksFair == 0) {
            return "Hoàn thành khá nhiệm vụ";
        }
        if (ratio >= 0.7) {
            return "Hoàn thành nhiệm vụ";
        }
        return "Không hoàn thành nhiệm vụ";
    }

    /** PL4B1!J15:J20 - score = ty le dong VI x 100. */
    static String memberClassification(double score, double bonus, double penalty) {
        if (score >= 100 && bonus > 0 && penalty == 0) {
            return "Chất lượng xuất sắc";
        }
        if (score >= 90) {
            return "Chất lượng tốt";
        }
        if (score >= 80) {
            return "Chất lượng khá";
        }
        if (score >= 50) {
            return "Chất lượng trung bình";
        }
        if (score > 0) {
            return "Chất lượng không đạt yêu cầu";
        }
        return null;
    }

    private static String kind(Item item) {
        return item.kind() == null ? "" : item.kind();
    }

    private static double rate(Item item) {
        return item.rate() == null ? 0 : item.rate();
    }

    private static double ratioOf(double value, double max) {
        return max == 0 ? 0 : value / max;
    }
}
