package com.taxworkbench.application.error;

import java.util.List;

public class PolicyViolationException extends ApiException {

    public PolicyViolationException(String message, List<ApiErrorDetail> details) {
        super("CLIENT_POLICY_VIOLATION", message, 422, details);
    }
}
