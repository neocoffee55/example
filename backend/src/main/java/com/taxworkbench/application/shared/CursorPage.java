package com.taxworkbench.application.shared;

import java.util.List;

public record CursorPage<T>(
        List<T> items,
        Page page
) {
    public record Page(
            String nextCursor,
            int pageSize,
            boolean hasNext
    ) {
    }
}
