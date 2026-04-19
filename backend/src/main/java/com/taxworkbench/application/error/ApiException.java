package com.taxworkbench.application.error;

import java.util.List;

public abstract class ApiException extends RuntimeException {

    private final String code;
    private final int status;
    private final List<ApiErrorDetail> details;

    protected ApiException(String code, String message, int status, List<ApiErrorDetail> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }

    public List<ApiErrorDetail> details() {
        return details;
    }
}
