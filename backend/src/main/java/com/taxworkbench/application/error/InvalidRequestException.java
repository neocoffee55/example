package com.taxworkbench.application.error;

import java.util.List;

public class InvalidRequestException extends ApiException {

    public InvalidRequestException(String message, List<ApiErrorDetail> details) {
        super("INVALID_REQUEST", message, 400, details);
    }
}
