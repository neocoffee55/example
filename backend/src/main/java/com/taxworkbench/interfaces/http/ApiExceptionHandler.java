package com.taxworkbench.interfaces.http;

import com.taxworkbench.application.error.ApiException;
import com.taxworkbench.application.error.ConflictException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ConflictErrorResponse> handleConflict(ConflictException exception) {
        return ResponseEntity.status(exception.status()).body(new ConflictErrorResponse(
                exception.code(),
                exception.getMessage(),
                exception.resourceId(),
                exception.currentVersion(),
                exception.updatedBy(),
                exception.updatedAt(),
                exception.fieldConflicts().stream()
                        .map(conflict -> new ConflictErrorResponse.FieldConflict(
                                conflict.field(),
                                conflict.baseValue(),
                                conflict.attemptedValue(),
                                conflict.currentValue()
                        ))
                        .toList()
        ));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> handleApi(ApiException exception) {
        return ResponseEntity.status(exception.status()).body(new ApiErrorResponse(
                exception.code(),
                exception.getMessage(),
                exception.details().stream()
                        .map(detail -> new ApiErrorResponse.Detail(detail.field(), detail.reason()))
                        .toList()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiErrorResponse.Detail> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiErrorResponse.Detail(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "INVALID_REQUEST",
                "Request validation failed.",
                details
        ));
    }
}
