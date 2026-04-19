package com.taxworkbench.application.workitem;

public record AuditLogQuery(
        Long workItemId,
        int pageSize,
        String cursor
) {
}
