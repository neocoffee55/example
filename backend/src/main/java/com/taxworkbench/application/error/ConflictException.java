package com.taxworkbench.application.error;

import java.time.Instant;
import java.util.List;

public class ConflictException extends ApiException {

    private final Long resourceId;
    private final long currentVersion;
    private final String updatedBy;
    private final Instant updatedAt;
    private final List<FieldConflict> fieldConflicts;

    public ConflictException(
            String code,
            String message,
            Long resourceId,
            long currentVersion,
            String updatedBy,
            Instant updatedAt,
            List<FieldConflict> fieldConflicts
    ) {
        super(code, message, 409, null);
        this.resourceId = resourceId;
        this.currentVersion = currentVersion;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.fieldConflicts = fieldConflicts == null ? List.of() : List.copyOf(fieldConflicts);
    }

    public Long resourceId() {
        return resourceId;
    }

    public long currentVersion() {
        return currentVersion;
    }

    public String updatedBy() {
        return updatedBy;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<FieldConflict> fieldConflicts() {
        return fieldConflicts;
    }

    public record FieldConflict(
            String field,
            String baseValue,
            String attemptedValue,
            String currentValue
    ) {
    }
}
