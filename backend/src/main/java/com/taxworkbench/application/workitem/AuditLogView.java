package com.taxworkbench.application.workitem;

import com.taxworkbench.domain.shared.AuditSource;

import java.time.Instant;

public record AuditLogView(
        Long auditLogId,
        Long workItemId,
        String fieldName,
        String beforeValue,
        String afterValue,
        String actorId,
        String actorName,
        AuditSource source,
        Instant changedAt
) {
}
