package com.taxworkbench.application.workitem;

import com.taxworkbench.domain.shared.ClientStatus;
import com.taxworkbench.domain.shared.ClientTier;
import com.taxworkbench.domain.shared.ClientType;
import com.taxworkbench.domain.shared.WorkItemStatus;
import com.taxworkbench.domain.shared.WorkItemType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WorkItemView(
        Long id,
        Long clientId,
        String clientName,
        String bizNo,
        WorkItemType type,
        WorkItemStatus status,
        String assignee,
        LocalDate dueDate,
        List<String> tags,
        String memo,
        Instant updatedAt,
        long version,
        ClientType clientType,
        ClientTier clientTier,
        ClientStatus clientStatus
) {
}
