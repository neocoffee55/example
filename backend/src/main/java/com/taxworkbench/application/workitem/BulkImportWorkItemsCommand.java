package com.taxworkbench.application.workitem;

import com.taxworkbench.domain.shared.WorkItemStatus;
import com.taxworkbench.domain.shared.WorkItemType;

import java.time.LocalDate;
import java.util.List;

public record BulkImportWorkItemsCommand(
        String requestId,
        List<BulkImportItem> items
) {
    public record BulkImportItem(
            Long clientId,
            WorkItemType type,
            WorkItemStatus status,
            String assignee,
            LocalDate dueDate,
            List<String> tags,
            String memo
    ) {
    }
}
