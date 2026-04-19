package com.taxworkbench.application.workitem;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record PatchWorkItemCommand(
        Long workItemId,
        long version,
        List<PatchOperation> operations
) {
    public record PatchOperation(
            String field,
            JsonNode value,
            JsonNode baseValue
    ) {
    }
}
