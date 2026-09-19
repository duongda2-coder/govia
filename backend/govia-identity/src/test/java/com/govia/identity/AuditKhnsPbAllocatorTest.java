package com.govia.identity;

import com.govia.audit.khkt.khnsnam.allocation.AuditKhnsPbAllocator;
import com.govia.audit.khkt.khnsnam.allocation.AuditKhnsPbAllocator.ScaleLevel;
import com.govia.audit.khkt.khnsnam.allocation.AuditKhnsPbAllocator.Staff;
import com.govia.audit.khkt.khnsnam.allocation.AuditKhnsPbAllocator.StaffAssignment;
import com.govia.audit.khkt.khnsnam.allocation.AuditKhnsPbAllocator.TeamObject;
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

    private Staff staff(String code, int grade, boolean lead, String... segments) {
        return new Staff(UUID.nameUUIDFromBytes(code.getBytes()), code, code, grade, Set.of(segments), lead, null, Set.of(), Set.of());
    }

    private Staff staff(String code, int grade, boolean lead, Set<String> blocked, String... segments) {
        return new Staff(UUID.nameUUIDFromBytes(code.getBytes()), code, code, grade, Set.of(segments), lead, null, blocked, Set.of());
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

    @Test
    void creditUnitOfMediumScaleGetsThreeCreditStaffAndOtherSegmentsOneEach() {
        List<Staff> staff = List.of(
                staff("L1", 2, true, "LN"),
                staff("C1", 1, false, "LN"), staff("C2", 1, false, "LN"), staff("C3", 1, false, "LN"),
                staff("A1", 1, false, "GA"), staff("D1", 1, false, "DP"));
        var result = allocator.allocate(List.of(object("CN1", Set.of(3), ScaleLevel.MEDIUM, "LN", "GA", "DP")), staff);

        assertThat(result.warnings()).isEmpty();
        assertThat(result.objectsFullyStaffed()).isEqualTo(1);
        // 3 tin dung (gom ca Truong doan) + TCKT + HDV = 5
        assertThat(teamOf(result, "CN1")).hasSize(5);
        assertThat(result.assignments().values().stream().filter(StaffAssignment::isLead)).hasSize(1);
    }

    @Test
    void creditUnitOfLowScaleGetsTwoCreditStaff() {
        List<Staff> staff = List.of(staff("L1", 1, true, "LN"), staff("C1", 1, false, "LN"), staff("C2", 1, false, "LN"));
        var result = allocator.allocate(List.of(object("CN1", Set.of(1), ScaleLevel.LOW, "LN")), staff);

        assertThat(result.warnings()).isEmpty();
        assertThat(teamOf(result, "CN1")).hasSize(2);
    }

    @Test
    void largeScaleRequiresGradeTwoOrHigher() {
        List<Staff> staff = List.of(
                staff("L1", 3, true, "LN"), staff("G1", 1, false, "LN"), staff("G2", 1, false, "LN"), staff("H1", 2, false, "LN"));
        var result = allocator.allocate(List.of(object("CN1", Set.of(1), ScaleLevel.LARGE, "LN")), staff);

        // 3 slot tin dung, deu can bac >= 2; chi co L1 (bac 3) va H1 (bac 2) -> thieu 1
        assertThat(teamOf(result, "CN1")).hasSize(2);
        assertThat(result.warnings()).hasSize(1);
        assertThat(result.objectsFullyStaffed()).isZero();
    }

    @Test
    void nonCreditUnitIsCappedAtFourStaffEvenWithManySegments() {
        List<Staff> staff = new ArrayList<>(List.of(
                staff("L1", 1, true, "GA"), staff("D1", 1, false, "DP"), staff("F1", 1, false, "FA"),
                staff("T1", 1, false, "TF"), staff("I1", 1, false, "IT", "CD")));
        var result = allocator.allocate(List.of(object("CN1", Set.of(2), ScaleLevel.LOW, "GA", "DP", "FA", "TF", "IT", "CD", "MF")), staff);

        assertThat(result.warnings()).isEmpty();
        assertThat(teamOf(result, "CN1")).hasSize(4);
    }

    @Test
    void oneStaffCannotBeInTwoTeamsInTheSameMonth() {
        List<Staff> staff = List.of(staff("L1", 1, true, "GA"), staff("L2", 1, true, "GA"));
        var result = allocator.allocate(List.of(
                object("CN1", Set.of(5), ScaleLevel.LOW, "GA"), object("CN2", Set.of(5), ScaleLevel.LOW, "GA")), staff);

        assertThat(result.objectsFullyStaffed()).isEqualTo(2);
        for (StaffAssignment a : result.assignments().values()) {
            assertThat(a.getMonthToObject()).hasSize(1);
        }
    }

    @Test
    void staffIsReusedInDifferentMonthsAndMultiMonthObjectKeepsSameTeam() {
        List<Staff> staff = List.of(staff("L1", 1, true, "GA"));
        var result = allocator.allocate(List.of(
                object("CN1", Set.of(1, 2), ScaleLevel.LOW, "GA"), object("CN2", Set.of(3), ScaleLevel.LOW, "GA")), staff);

        assertThat(result.objectsFullyStaffed()).isEqualTo(2);
        Map<Integer, String> months = result.assignments().values().iterator().next().getMonthToObject();
        assertThat(months).containsEntry(1, "CN1").containsEntry(2, "CN1").containsEntry(3, "CN2");
    }

    @Test
    void blockedStaffAreNeverAssignedToThatObject() {
        List<Staff> staff = List.of(staff("L1", 1, true, Set.of("CN1"), "GA"), staff("L2", 1, true, "GA"));
        var result = allocator.allocate(List.of(object("CN1", Set.of(1), ScaleLevel.LOW, "GA")), staff);

        assertThat(result.assignments()).hasSize(1);
        assertThat(result.assignments().containsKey(UUID.nameUUIDFromBytes("L1".getBytes()))).isFalse();
    }

    @Test
    void equallySuitableStaffArePickedRandomlyButRulesStillHold() {
        List<Staff> staff = List.of(
                staff("L1", 1, true, "GA"), staff("L2", 1, true, "GA"), staff("L3", 1, true, "GA"), staff("L4", 1, true, "GA"));
        Set<Set<String>> distinctTeams = new HashSet<>();
        for (long seed = 0; seed < 30; seed++) {
            var result = new AuditKhnsPbAllocator(new Random(seed)).allocate(List.of(object("CN1", Set.of(1), ScaleLevel.LOW, "GA")), staff);
            assertThat(result.warnings()).isEmpty();
            assertThat(result.assignments()).hasSize(1);
            distinctTeams.add(Set.copyOf(teamOf(result, "CN1")));
        }
        // 4 ung vien cung diem -> qua nhieu lan phan bo phai ra it nhat 2 phuong an khac nhau (khong con co dinh theo ma)
        assertThat(distinctTeams.size()).isGreaterThan(1);
    }
}
