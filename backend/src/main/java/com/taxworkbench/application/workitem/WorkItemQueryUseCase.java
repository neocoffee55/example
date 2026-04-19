package com.taxworkbench.application.workitem;

import com.taxworkbench.application.shared.CursorPage;

public interface WorkItemQueryUseCase {

    CursorPage<WorkItemView> listWorkItems(WorkItemListQuery query);

    CursorPage<AuditLogView> listAuditLogs(AuditLogQuery query);
}
