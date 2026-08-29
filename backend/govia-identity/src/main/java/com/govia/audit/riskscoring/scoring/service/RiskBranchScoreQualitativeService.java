package com.govia.audit.riskscoring.scoring.service;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectUnit;
import com.govia.audit.riskscoring.masterdata.entity.RiskCriteriaQualitative;
import com.govia.audit.riskscoring.masterdata.entity.RiskFrequencyCoefficient;
import com.govia.audit.riskscoring.masterdata.entity.RiskGroup2;
import com.govia.audit.riskscoring.masterdata.entity.RiskMatrix;
import com.govia.audit.riskscoring.masterdata.entity.RiskScoreRank;
import com.govia.audit.riskscoring.masterdata.repository.AuditObjectUnitRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskCriteriaQualitativeRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskFrequencyCoefficientRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskGroup2Repository;
import com.govia.audit.riskscoring.masterdata.repository.RiskMatrixRepository;
import com.govia.audit.riskscoring.masterdata.repository.RiskScoreRankRepository;
import com.govia.audit.riskscoring.scoring.dto.RiskBranchScoreQualitativeRowResponse;
import com.govia.audit.riskscoring.scoring.entity.RiskCriteriaQualitativeValue;
import com.govia.audit.riskscoring.scoring.repository.RiskCriteriaQualitativeValueRepository;
import com.govia.core.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * "Ket qua cham diem rui ro dinh tinh" theo chi nhanh/nam (sheet CT_Diem_DT, tinh dong tu du lieu
 * da nhap o "Ho so rui ro dinh tinh" - xem RiskCriteriaQualitativeValueService). Man hinh CHI XEM.
 *
 * <p>Voi moi chi tieu (RiskCriteriaQualitative) active:
 * <ol>
 *   <li>Diem tan suat: tra {@code likelihoodLevel} vao {@link RiskMatrix#getFrequencyLevel()},
 *   chon cot theo {@code impactLevel} (1=thap, 2=trung binh, 3=cao -&gt; scoreLowSeverity/
 *   Medium/HighSeverity).</li>
 *   <li>Diem he so sai pham lich su: xet chuoi "violation" (0/1/N/rong) cua chinh tieu do o CHI
 *   NHANH nay trong 5 nam LIEN TRUOC nam cham (year-5..year-1), phan loai theo 5 truong hop (xem
 *   {@link #classifyHistory}), tra {@link RiskFrequencyCoefficient} theo {@code code} = gia tri
 *   truong hop (0-4, quy uoc: TH1=4, TH2=3, TH3=2, TH4=1, TH5=0) de lay bonusPoint, cong them
 *   diem RR lap lai (repeatRiskPoint, tra theo repeatCount = so lan "1" - 1) neu TH2/TH3.</li>
 *   <li>Dong gop = min(100, diem tan suat + diem he so) x {@code criteria.weight} x
 *   {@code group2.weight} (qua {@code criteria.group2Id}).</li>
 * </ol>
 * Cot hien thi gom theo group2 (tong dong gop cac chi tieu cung group2). Tong diem = tong moi
 * group2. Xep hang = tra RiskScoreRank theo Tong diem + nam (cung pattern voi
 * RiskAssessmentOtherRankingService.resolveRankLabel).
 *
 * <p><b>Luu y:</b> day la phan suy luan phuc tap nhat trong tai lieu goc, mot so cho van ban dien
 * dat chua thuc su ro rang (vi du truong hop "diem sau diem 1 cuoi" khi lan "1" cuoi roi vao nam
 * cuoi cung cua so 5 nam, khong con nam nao "sau" de xet) - da chon cach hieu hop ly nhat va ghi
 * ro tung buoc, can doi chieu so lieu thuc te sau khi chay thu.
 */
@Service
public class RiskBranchScoreQualitativeService {

    private final RiskCriteriaQualitativeValueRepository valueRepository;
    private final RiskCriteriaQualitativeRepository criteriaRepository;
    private final RiskGroup2Repository group2Repository;
    private final RiskMatrixRepository matrixRepository;
    private final RiskFrequencyCoefficientRepository frequencyCoefficientRepository;
    private final AuditObjectUnitRepository auditObjectUnitRepository;
    private final RiskScoreRankRepository rankRepository;

    public RiskBranchScoreQualitativeService(RiskCriteriaQualitativeValueRepository valueRepository,
                                              RiskCriteriaQualitativeRepository criteriaRepository,
                                              RiskGroup2Repository group2Repository,
                                              RiskMatrixRepository matrixRepository,
                                              RiskFrequencyCoefficientRepository frequencyCoefficientRepository,
                                              AuditObjectUnitRepository auditObjectUnitRepository,
                                              RiskScoreRankRepository rankRepository) {
        this.valueRepository = valueRepository;
        this.criteriaRepository = criteriaRepository;
        this.group2Repository = group2Repository;
        this.matrixRepository = matrixRepository;
        this.frequencyCoefficientRepository = frequencyCoefficientRepository;
        this.auditObjectUnitRepository = auditObjectUnitRepository;
        this.rankRepository = rankRepository;
    }

    @Transactional(readOnly = true)
    public List<RiskBranchScoreQualitativeRowResponse> list(Integer year) {
        UUID tenantId = TenantContext.getTenantId();
        int fromYear = year - 5;
        int toYear = year - 1;

        List<RiskCriteriaQualitative> criteria = criteriaRepository.findByTenantIdOrderByCodeAsc(tenantId).stream()
                .filter(RiskCriteriaQualitative::isActive)
                .toList();
        Map<UUID, RiskGroup2> groups2 = group2ById(tenantId);
        Map<Integer, RiskMatrix> matrixByFrequency = matrixByFrequencyLevel(tenantId);
        List<RiskFrequencyCoefficient> caseCoefficients = frequencyCoefficientRepository.findByTenantIdOrderByCodeAsc(tenantId);
        Map<String, RiskFrequencyCoefficient> caseByCode = new HashMap<>();
        List<RiskFrequencyCoefficient> repeatCoefficients = new ArrayList<>();
        for (RiskFrequencyCoefficient c : caseCoefficients) {
            if (c.isRepeat()) {
                repeatCoefficients.add(c);
            } else {
                caseByCode.put(c.getCode(), c);
            }
        }
        Map<String, AuditObjectUnit> units = unitsByCode(tenantId);
        List<RiskScoreRank> ranks = rankRepository.findByTenantIdOrderByFromYearAscScoreFromAsc(tenantId);

        // history[branchCode][criteriaId][nam] = violation ("0"/"1"/"N"/...), gom du lieu ca 5 nam
        // truoc va nam hien tai (dung de xac dinh danh sach chi nhanh "co ho so")
        Map<String, Map<UUID, Map<Integer, String>>> history = new HashMap<>();
        Set<String> branchesWithData = new HashSet<>();
        for (RiskCriteriaQualitativeValue v : valueRepository.findByTenantIdAndYearBetween(tenantId, fromYear, year)) {
            branchesWithData.add(v.getBranchCode());
            history.computeIfAbsent(v.getBranchCode(), k -> new HashMap<>())
                    .computeIfAbsent(v.getCriteriaId(), k -> new HashMap<>())
                    .put(v.getYear(), v.getViolation());
        }

        List<RiskBranchScoreQualitativeRowResponse> result = new ArrayList<>();
        for (String branchCode : branchesWithData.stream().sorted().toList()) {
            Map<UUID, Map<Integer, String>> byCriteria = history.getOrDefault(branchCode, Map.of());
            Map<String, BigDecimal> scoresByGroup2 = new HashMap<>();
            BigDecimal total = BigDecimal.ZERO;
            for (RiskCriteriaQualitative criterion : criteria) {
                RiskGroup2 group2 = criterion.getGroup2Id() != null ? groups2.get(criterion.getGroup2Id()) : null;
                if (group2 == null || group2.getWeight() == null || criterion.getWeight() == null) {
                    continue;
                }
                BigDecimal matrixScore = matrixScore(matrixByFrequency, criterion);
                if (matrixScore == null) {
                    matrixScore = BigDecimal.ZERO;
                }
                List<String> violations = fiveYearHistory(byCriteria.get(criterion.getId()), fromYear, toYear);
                BigDecimal historyBonus = historyBonus(violations, caseByCode, repeatCoefficients);
                BigDecimal raw = matrixScore.add(historyBonus);
                if (raw.compareTo(BigDecimal.valueOf(100)) > 0) {
                    raw = BigDecimal.valueOf(100);
                }
                BigDecimal contribution = raw.multiply(criterion.getWeight()).multiply(group2.getWeight());
                scoresByGroup2.merge(group2.getCode(), contribution, BigDecimal::add);
                total = total.add(contribution);
            }
            AuditObjectUnit unit = units.get(branchCode);
            total = total.setScale(2, RoundingMode.HALF_UP);
            String rankLabel = resolveRankLabel(ranks, total, year);
            result.add(new RiskBranchScoreQualitativeRowResponse(branchCode, unit != null ? unit.getName() : null,
                    year, total, rankLabel, scoresByGroup2));
        }
        result.sort((a, b) -> b.totalScore().compareTo(a.totalScore()));
        return result;
    }

    private BigDecimal matrixScore(Map<Integer, RiskMatrix> matrixByFrequency, RiskCriteriaQualitative criterion) {
        if (criterion.getLikelihoodLevel() == null || criterion.getImpactLevel() == null) {
            return null;
        }
        RiskMatrix row = matrixByFrequency.get(criterion.getLikelihoodLevel());
        if (row == null) {
            return null;
        }
        return switch (criterion.getImpactLevel()) {
            case 1 -> row.getScoreLowSeverity();
            case 2 -> row.getScoreMediumSeverity();
            case 3 -> row.getScoreHighSeverity();
            default -> null;
        };
    }

    /** violations theo thu tu tang dan nam (index 0 = fromYear .. index 4 = toYear = nam-1). */
    private List<String> fiveYearHistory(Map<Integer, String> byYear, int fromYear, int toYear) {
        List<String> list = new ArrayList<>();
        for (int y = fromYear; y <= toYear; y++) {
            list.add(byYear != null ? byYear.get(y) : null);
        }
        return list;
    }

    private BigDecimal historyBonus(List<String> violations, Map<String, RiskFrequencyCoefficient> caseByCode,
                                     List<RiskFrequencyCoefficient> repeatCoefficients) {
        HistoricalCase result = classifyHistory(violations);
        if (result == null) {
            return BigDecimal.ZERO;
        }
        RiskFrequencyCoefficient caseRow = caseByCode.get(String.valueOf(result.caseValue));
        BigDecimal bonus = caseRow != null && caseRow.getBonusPoint() != null ? caseRow.getBonusPoint() : BigDecimal.ZERO;
        if (result.repeatEligible && result.onesCount > 1) {
            bonus = bonus.add(findRepeatBonus(repeatCoefficients, result.onesCount - 1));
        }
        return bonus;
    }

    private record HistoricalCase(int caseValue, boolean repeatEligible, int onesCount) {
    }

    /** Phan loai 5 nam lich su theo dung 5 truong hop mo ta trong sheet CT_Diem_DT (muc "8.d").
     * Tra ve null neu khong khop truong hop nao (bo qua diem he so cho chi tieu do). */
    private HistoricalCase classifyHistory(List<String> violations) {
        boolean allBlank = violations.stream().allMatch(v -> v == null || v.isBlank());
        if (allBlank) {
            return new HistoricalCase(4, false, 0); // TH1: chua kiem toan
        }
        int ones = 0;
        int zeros = 0;
        int lastOneIdx = -1;
        for (int i = 0; i < violations.size(); i++) {
            String v = violations.get(i);
            if ("1".equals(v)) {
                ones++;
                lastOneIdx = i;
            } else if ("0".equals(v)) {
                zeros++;
            }
        }
        String nextAfterLastOne = (lastOneIdx >= 0 && lastOneIdx + 1 < violations.size()) ? violations.get(lastOneIdx + 1) : null;
        boolean nextIsZero = "0".equals(nextAfterLastOne);

        if (ones >= 1 && ones <= 4 && zeros >= 1 && nextIsZero) {
            return new HistoricalCase(3, true, ones); // TH2: co loi, khong lap lai
        }
        if (ones >= 2 && ones <= 5 && !nextIsZero) {
            return new HistoricalCase(2, true, ones); // TH3: co loi va lap lai
        }
        if (ones == 1) {
            boolean allAfterNonZero = true;
            for (int i = lastOneIdx + 1; i < violations.size(); i++) {
                if ("0".equals(violations.get(i))) {
                    allAfterNonZero = false;
                    break;
                }
            }
            if (allAfterNonZero) {
                return new HistoricalCase(1, false, ones); // TH4: loi lan dau
            }
        }
        if (ones == 0 && zeros > 0) {
            return new HistoricalCase(0, false, ones); // TH5: kiem toan khong loi
        }
        return null;
    }

    /** repeatCount tren RiskFrequencyCoefficient co the la so nguyen ("1".."4") hoac dang mo ">=5"
     * (xem Javadoc entity RiskFrequencyCoefficient). */
    private BigDecimal findRepeatBonus(List<RiskFrequencyCoefficient> repeatCoefficients, int repeatCount) {
        for (RiskFrequencyCoefficient row : repeatCoefficients) {
            String rc = row.getRepeatCount();
            if (rc == null || rc.isBlank()) {
                continue;
            }
            rc = rc.trim();
            try {
                if (rc.startsWith(">=")) {
                    if (repeatCount >= Integer.parseInt(rc.substring(2).trim())) {
                        return row.getRepeatRiskPoint() != null ? row.getRepeatRiskPoint() : BigDecimal.ZERO;
                    }
                } else if (Integer.parseInt(rc) == repeatCount) {
                    return row.getRepeatRiskPoint() != null ? row.getRepeatRiskPoint() : BigDecimal.ZERO;
                }
            } catch (NumberFormatException ignored) {
                // dong danh muc bi nhap sai dinh dang - bo qua
            }
        }
        return BigDecimal.ZERO;
    }

    /** Xet khoang diem [scoreFrom, scoreTo] con hieu luc trong nam (fromYear..toYear) - lay dong dau tien khop. */
    private String resolveRankLabel(List<RiskScoreRank> ranks, BigDecimal score, Integer year) {
        return ranks.stream()
                .filter(RiskScoreRank::isActive)
                .filter(r -> year >= r.getFromYear() && year <= r.getToYear())
                .filter(r -> score.compareTo(r.getScoreFrom()) >= 0 && score.compareTo(r.getScoreTo()) <= 0)
                .map(RiskScoreRank::getRankLabel)
                .findFirst()
                .orElse(null);
    }

    private Map<UUID, RiskGroup2> group2ById(UUID tenantId) {
        Map<UUID, RiskGroup2> map = new HashMap<>();
        group2Repository.findByTenantIdOrderByCodeAsc(tenantId).forEach(g -> map.put(g.getId(), g));
        return map;
    }

    private Map<Integer, RiskMatrix> matrixByFrequencyLevel(UUID tenantId) {
        Map<Integer, RiskMatrix> map = new HashMap<>();
        matrixRepository.findByTenantIdOrderByFrequencyLevelAsc(tenantId).forEach(m -> map.put(m.getFrequencyLevel(), m));
        return map;
    }

    private Map<String, AuditObjectUnit> unitsByCode(UUID tenantId) {
        Map<String, AuditObjectUnit> map = new HashMap<>();
        auditObjectUnitRepository.findByTenantIdOrderByCodeAsc(tenantId).forEach(u -> map.put(u.getCode(), u));
        return map;
    }
}
