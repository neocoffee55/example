package com.taxworkbench.interfaces.http;

import java.time.Instant;
import java.util.List;

public record ConflictErrorResponse(
        String code,
        String message,
        Long resourceId,
        long currentVersion,
        String updatedBy,
        Instant updatedAt,
        List<FieldConflict> fieldConflicts
) {
    public record FieldConflict(
            String field,
            String baseValue,
            String attemptedValue,
            String currentValue
    ) {
    }
}
