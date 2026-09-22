package com.govia.audit.phbc.lookup;

import com.govia.audit.phbc.common.PhbcMasterData;
import com.govia.core.web.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Cac danh sach chon (list) dung chung cho man hinh Phat hanh bao cao - 1 lan goi. */
@RestController
@RequestMapping("/api/audit/phbc/lookups")
public class PhbcLookupController {

    public record Option(UUID id, String code, String name) {
    }

    public record UnitOption(UUID id, String code, String name) {
    }

    public record Lookups(List<Option> businessSegments, List<UnitOption> executingUnits) {
    }

    private final PhbcMasterData masterData;

    public PhbcLookupController(PhbcMasterData masterData) {
        this.masterData = masterData;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_AUDIT.PHBC.VIEW')")
    public ApiResponse<Lookups> lookups() {
        List<Option> businessSegments = masterData.businessSegments().values().stream()
                .map(i -> new Option(i.getId(), i.getCode(), i.getName())).toList();
        List<UnitOption> executingUnits = masterData.executingUnits().values().stream()
                .map(u -> new UnitOption(u.getId(), u.getCode(), u.getName())).toList();
        return ApiResponse.ok(new Lookups(businessSegments, executingUnits));
    }
}
