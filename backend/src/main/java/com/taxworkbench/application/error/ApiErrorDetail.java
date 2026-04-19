package com.taxworkbench.application.error;

public record ApiErrorDetail(
        String field,
        String reason
) {
}
