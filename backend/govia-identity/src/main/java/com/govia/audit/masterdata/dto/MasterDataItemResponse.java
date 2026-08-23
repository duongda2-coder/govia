package com.govia.audit.masterdata.dto;

import java.time.LocalDate;
import java.util.UUID;

public record MasterDataItemResponse(
        UUID id,
        String category,
        String code,
        String name,
        String description,
        UUID parentId,
        LocalDate validFrom,
        LocalDate validTo,
        Integer sortOrder,
        boolean active
) {
}
