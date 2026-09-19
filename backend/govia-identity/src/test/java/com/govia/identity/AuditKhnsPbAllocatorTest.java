package com.govia.identity;

import com.govia.audit.khkt.khnsnam.allocation.AuditKhnsPbAllocator;
import com.govia.audit.khkt.khnsnam.allocation.AuditKhnsPbAllocator.ObjectRole;
import com.govia.audit.khkt.khnsnam.allocation.AuditKhnsPbAllocator.ScaleLevel;
import com.govia.audit.khkt.khnsnam.allocation.AuditKhnsPbAllocator.Staff;
import com.govia.audit.khkt.khnsnam.allocation.AuditKhnsPbAllocator.StaffAssignment;
import com.govia.audit.khkt.khnsnam.allocation.AuditKhnsPbAllocator.TeamObject;
import com.govia.audit.khkt.khnsnam.entity.AuditKhnsPosition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditKhnsPbAllocatorTest {

    private final AuditKhnsPbAllocator allocator = new AuditKhnsPbAllocator();

    private static UUID idOf(String code) {
        return UUID.nameUUIDFromBytes(code.getBytes());
    }

    private Staff staff(String code, int grade, boolean lead, String... segments) {
        return new Staff(idOf(code), code, code, grade, Set.of(segments), lead, false, null, Set.of(), Set.of());
    }

    private Staff staff(String code, int grade, boolean lead, Set<String> blocked, String... segments) {
        return new Staff(idOf(code), code, code, grade, Set.of(segments), lead, false, null, blocked, Set.of());
    }

    private Staff groupLeadStaff(String code, int grade, String... segments) {
        return new Staff(idOf(code), code, code, grade, Set.of(segments), false, true, null, Set.of(), Set.of());
    }

    private TeamObject object(String code, Set<Integer> months, ScaleLevel credit, String... segments) {
        return new TeamObject(code, code, months, Set.of(segments), credit, ScaleLevel.LOW);
    }

    private List<String> teamOf(AuditKhnsPbAllocator.Result result, String objectCode) {
        List<String> team = new ArrayList<>();
        result.assignments().forEach((id, a) -> {
            if (a.getObjectCodes().contains(objectCode)) {
                team.add(id.toString());
            }
        });
        return team;
    }

    private ObjectRole role(AuditKhnsPbAllocator.Result result, String staffCode, String objectCode) {
        return result.assignments().get(idOf(staffCode)).getObjectRoles().get(objectCode);
    }

    @Test
    void creditUnitOfMediumScaleGetsThreeCreditStaffAndOtherSegmentsOneEach() {
        List<Staff> staff = List.of(
                staff("L1", 2, true, "LN"),
                staff("C1", 1, false, "LN"), staff("C2", 1, false, "LN"), staff("C3", 1, false, "LN"),
                staff("A1", 1, false, "GA"), staff("D1", 1, false, "DP"));
        var result = allocator.allocate(List.of(object("CN1", Set.of(3), ScaleLevel.MEDIUM, "LN", "GA", "DP")), staff);

        assertThat(result.warnings()).isEmpty();
        assertThat(result.objectsFullyStaffed()).isEqualTo(1);
        // Truong doan (lam QTDH, khong chiem vi tri) + 3 tin dung + TCKT + HDV = 6
        assertThat(teamOf(result, "CN1")).hasSize(6);
        assertThat(result.assignments().values().stream().filter(StaffAssignment::isLead)).hasSize(1);
    }

    @Test
    void creditUnitOfLowScaleGetsTwoCreditStaffPlusTeamLead() {
        List<Staff> staff = List.of(staff("L1", 1, true, "LN"), staff("C1", 1, false, "LN"), staff("C2", 1, false, "LN"));
        var result = allocator.allocate(List.of(object("CN1", Set.of(1), ScaleLevel.LOW, "LN")), staff);

        assertThat(result.warnings()).isEmpty();
        assertThat(teamOf(result, "CN1")).hasSize(3);
    }

    @Test
    void largeScaleRequiresGradeTwoOrHigher() {
        List<Staff> staff = List.of(
                staff("L1", 3, true, "LN"), staff("G1", 1, false, "LN"), staff("G2", 1, false, "LN"),
                staff("H1", 2, false, "LN"), staff("H2", 3, false, "LN"));
        var result = allocator.allocate(List.of(object("CN1", Set.of(1), ScaleLevel.LARGE, "LN")), staff);

        // 3 slot tin dung, deu can bac >= 2; ngoai Truong doan L1 chi co H1 (bac 2) va H2 (bac 3) -> thieu 1
        assertThat(teamOf(result, "CN1")).hasSize(3);
        assertThat(result.warnings()).hasSize(1);
        assertThat(result.objectsFullyStaffed()).isZero();
    }

    @Test
    void nonCreditUnitIsCappedAtFourStaffEvenWithManySegments() {
        List<Staff> staff = new ArrayList<>(List.of(
                staff("L1", 1, true, "GA"), staff("A1", 1, false, "GA"), staff("D1", 1, false, "DP"), staff("F1", 1, false, "FA"),
                staff("T1", 1, false, "TF"), staff("I1", 1, false, "IT", "CD")));
        var result = allocator.allocate(List.of(object("CN1", Set.of(2), ScaleLevel.LOW, "GA", "DP", "FA", "TF", "IT", "CD", "MF")), staff);

        assertThat(result.warnings()).isEmpty();
        assertThat(teamOf(result, "CN1")).hasSize(4);
    }

    @Test
    void oneStaffCannotBeInTwoTeamsInTheSameMonth() {
        List<Staff> staff = List.of(
                staff("L1", 1, true, "GA"), staff("L2", 1, true, "GA"), staff("M1", 1, false, "GA"), staff("M2", 1, false, "GA"));
        var result = allocator.allocate(List.of(
                object("CN1", Set.of(5), ScaleLevel.LOW, "GA"), object("CN2", Set.of(5), ScaleLevel.LOW, "GA")), staff);

        assertThat(result.objectsFullyStaffed()).isEqualTo(2);
        for (StaffAssignment a : result.assignments().values()) {
            assertThat(a.getMonthToObject()).hasSize(1);
        }
    }

    @Test
    void staffIsReusedInDifferentMonthsAndMultiMonthObjectKeepsSameTeam() {
        List<Staff> staff = List.of(staff("L1", 1, true, "GA"), staff("M1", 1, false, "GA"));
        var result = allocator.allocate(List.of(
                object("CN1", Set.of(1, 2), ScaleLevel.LOW, "GA"), object("CN2", Set.of(3), ScaleLevel.LOW, "GA")), staff);

        assertThat(result.objectsFullyStaffed()).isEqualTo(2);
        Map<Integer, String> months = result.assignments().get(idOf("L1")).getMonthToObject();
        assertThat(months).containsEntry(1, "CN1").containsEntry(2, "CN1").containsEntry(3, "CN2");
    }

    @Test
    void blockedStaffAreNeverAssignedToThatObject() {
        List<Staff> staff = List.of(staff("L1", 1, true, Set.of("CN1"), "GA"), staff("L2", 1, true, "GA"), staff("M1", 1, false, "GA"));
        var result = allocator.allocate(List.of(object("CN1", Set.of(1), ScaleLevel.LOW, "GA")), staff);

        assertThat(result.assignments()).hasSize(2);
        assertThat(result.assignments().containsKey(idOf("L1"))).isFalse();
    }

    @Test
    void equallySuitableStaffArePickedRandomlyButRulesStillHold() {
        List<Staff> staff = List.of(
                staff("L1", 1, true, "GA"), staff("L2", 1, true, "GA"), staff("L3", 1, true, "GA"), staff("L4", 1, true, "GA"));
        Set<Set<String>> distinctTeams = new HashSet<>();
        for (long seed = 0; seed < 30; seed++) {
            var result = new AuditKhnsPbAllocator(new Random(seed)).allocate(List.of(object("CN1", Set.of(1), ScaleLevel.LOW, "GA")), staff);
            assertThat(result.warnings()).isEmpty();
            assertThat(result.assignments()).hasSize(2); // Truong doan + 1 TCKT
            distinctTeams.add(Set.copyOf(teamOf(result, "CN1")));
        }
        // 4 ung vien cung diem -> qua nhieu lan phan bo phai ra it nhat 2 phuong an khac nhau (khong con co dinh theo ma)
        assertThat(distinctTeams.size()).isGreaterThan(1);
    }

    @Test
    void teamLeadAlsoHoldsQtdhGroupLeadAndQtdhMemberPositions() {
        List<Staff> staff = List.of(staff("L1", 2, true, "LN"), staff("C1", 1, false, "LN"), staff("C2", 1, false, "LN"));
        var result = allocator.allocate(List.of(object("CN1", Set.of(1), ScaleLevel.LOW, "LN")), staff);

        ObjectRole lead = role(result, "L1", "CN1");
        assertThat(lead.getPositions()).containsExactlyInAnyOrder(
                AuditKhnsPosition.TEAM_LEAD, AuditKhnsPosition.QTDH_GROUP_LEAD, AuditKhnsPosition.QTDH_MEMBER);
        assertThat(lead.getSegments()).containsExactly("CE");
    }

    @Test
    void eachCreditAndNtdGroupGetsExactlyOneGroupLeadPreferringGroupLeadCapableStaff() {
        List<Staff> staff = List.of(
                staff("L1", 2, true, "LN"),
                staff("C1", 1, false, "LN"), groupLeadStaff("C2", 1, "LN"), staff("C3", 1, false, "LN"),
                staff("A1", 1, false, "GA"), groupLeadStaff("D1", 1, "DP"));
        var result = allocator.allocate(List.of(object("CN1", Set.of(3), ScaleLevel.MEDIUM, "LN", "GA", "DP")), staff);

        assertThat(result.warnings()).isEmpty();
        long creditGroupLeads = result.assignments().values().stream()
                .filter(a -> a.getObjectRoles().get("CN1").getPositions().contains(AuditKhnsPosition.TD_GROUP_LEAD)).count();
        long ntdGroupLeads = result.assignments().values().stream()
                .filter(a -> a.getObjectRoles().get("CN1").getPositions().contains(AuditKhnsPosition.NTD_GROUP_LEAD)).count();
        assertThat(creditGroupLeads).isEqualTo(1);
        assertThat(ntdGroupLeads).isEqualTo(1);
        // Truong nhom cung la thanh vien cua nhom, va duoc chon trong nguoi co kha nang Truong nhom
        assertThat(role(result, "C2", "CN1").getPositions()).containsExactlyInAnyOrder(AuditKhnsPosition.TD_GROUP_LEAD, AuditKhnsPosition.TD_MEMBER);
        assertThat(role(result, "D1", "CN1").getPositions()).containsExactlyInAnyOrder(AuditKhnsPosition.NTD_GROUP_LEAD, AuditKhnsPosition.NTD_MEMBER);
        assertThat(role(result, "C1", "CN1").getPositions()).containsExactly(AuditKhnsPosition.TD_MEMBER);
        assertThat(role(result, "A1", "CN1").getPositions()).containsExactly(AuditKhnsPosition.NTD_MEMBER);
        assertThat(role(result, "A1", "CN1").getSegments()).containsExactly("GA");
    }

    @Test
    void staffWorkEitherCreditOrNtdAndAtMostThreeSegmentsEvenWhenCapableOfEverything() {
        Staff everything = new Staff(idOf("X"), "X", "X", 3, Set.of("LN", "GA", "FA", "TF", "DP", "AM", "IT", "CD", "MF", "CE"),
                false, true, null, Set.of(), Set.of());
        List<Staff> staff = List.of(staff("L1", 3, true, "LN"), everything, staff("Y", 3, false, "LN", "GA", "FA", "DP", "IT", "CD", "MF"),
                staff("Z", 3, false, "LN", "GA", "TF"));
        for (long seed = 0; seed < 20; seed++) {
            var result = new AuditKhnsPbAllocator(new Random(seed)).allocate(
                    List.of(object("CN1", Set.of(1), ScaleLevel.LOW, "LN", "GA", "FA", "TF", "DP", "IT", "CD", "MF")), staff);
            for (StaffAssignment a : result.assignments().values()) {
                for (ObjectRole r : a.getObjectRoles().values()) {
                    assertThat(r.getSegments().size()).isLessThanOrEqualTo(AuditKhnsPosition.MAX_SEGMENTS);
                    boolean credit = r.getSegments().contains("LN");
                    boolean ntd = r.getSegments().stream().anyMatch(c -> !c.equals("LN") && !c.equals("CE"));
                    assertThat(credit && ntd).isFalse();
                    assertThat(r.getPositions().stream().anyMatch(AuditKhnsPosition::isCredit)
                            && r.getPositions().stream().anyMatch(AuditKhnsPosition::isNonCredit)).isFalse();
                }
            }
        }
    }

    @Test
    void ntdSegmentWithoutItsOwnSlotIsAbsorbedByCapableNtdMemberUpToThreeSegments() {
        // chi co IT (1 trong 3 nghiep vu CNTT/The/TTKQ) nen khong co vi tri rieng -> giao them cho thanh vien NTD dam nhan duoc
        List<Staff> staff = List.of(staff("L1", 1, true, "GA"), staff("A1", 1, false, "GA", "IT"));
        var result = allocator.allocate(List.of(object("CN1", Set.of(2), ScaleLevel.LOW, "GA", "IT")), staff);

        assertThat(result.warnings()).isEmpty();
        assertThat(role(result, "A1", "CN1").getSegments()).containsExactlyInAnyOrder("GA", "IT");
    }
}
