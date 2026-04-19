package com.taxworkbench.application.workitem;

import java.util.List;

public record BulkImportResult(
        String requestId,
        Summary summary,
        List<Failure> failures
) {
    public record Summary(
            int received,
            int created,
            int failed
    ) {
    }

    public record Failure(
            int row,
            String code,
            String message
    ) {
    }
}
