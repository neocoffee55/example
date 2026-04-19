package com.taxworkbench.application.workitem;

import com.taxworkbench.domain.shared.WorkItemStatus;
import com.taxworkbench.domain.shared.WorkItemType;

import java.time.LocalDate;
import java.util.List;

public record CreateWorkItemCommand(
        Long clientId,
        WorkItemType type,
        WorkItemStatus status,
        String assignee,
        LocalDate dueDate,
        List<String> tags,
        String memo
) {
}
