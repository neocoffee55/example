package com.taxworkbench.application.workitem;

import java.io.OutputStream;

public interface WorkItemCommandUseCase {

    WorkItemView createWorkItem(CreateWorkItemCommand command);

    WorkItemView patchWorkItem(PatchWorkItemCommand command);

    BulkImportResult bulkImport(BulkImportWorkItemsCommand command);

    void exportWorkItems(WorkItemListQuery query, OutputStream outputStream);
}
