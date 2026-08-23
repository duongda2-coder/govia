package com.govia.audit.masterdata.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public record MasterDataItemRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        UUID parentId,
        LocalDate validFrom,
        LocalDate validTo,
        Integer sortOrder,
        boolean active
) {
}
