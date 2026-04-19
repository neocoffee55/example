package com.taxworkbench.interfaces.http;

import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        List<Detail> details
) {
    public record Detail(
            String field,
            String reason
    ) {
    }
}
