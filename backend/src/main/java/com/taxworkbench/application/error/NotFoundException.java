package com.taxworkbench.application.error;

public class NotFoundException extends ApiException {

    public NotFoundException(String code, String message) {
        super(code, message, 404, null);
    }
}
