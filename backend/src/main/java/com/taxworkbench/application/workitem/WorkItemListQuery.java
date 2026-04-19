package com.taxworkbench.application.workitem;

import com.taxworkbench.domain.shared.ClientTier;
import com.taxworkbench.domain.shared.ClientType;
import com.taxworkbench.domain.shared.WorkItemStatus;

import java.time.LocalDate;
import java.util.List;

public record WorkItemListQuery(
        String clientName,
        List<WorkItemStatus> statuses,
        List<String> assignees,
        LocalDate dueDateFrom,
        LocalDate dueDateTo,
        ClientType clientType,
        ClientTier clientTier,
        String sort,
        int pageSize,
        String cursor
) {
}
