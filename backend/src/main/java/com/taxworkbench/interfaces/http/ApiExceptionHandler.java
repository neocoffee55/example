package com.taxworkbench.interfaces.http;

import com.taxworkbench.application.WorkItemConflictException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(WorkItemConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, Object> handleWorkItemConflict(WorkItemConflictException exception) {
        return Map.of(
                "entityId", exception.getEntityId(),
                "serverRevision", exception.getServerRevision(),
                "conflictFields", exception.getConflictFields(),
                "serverSnapshot", exception.getServerSnapshot(),
                "attemptedChanges", exception.getAttemptedChanges(),
                "message", exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> handleBadRequest(IllegalArgumentException exception) {
        return Map.of(
                "message", exception.getMessage()
        );
    }
}
