package com.taxworkbench.application;

import java.util.List;
import java.util.Map;

public class WorkItemConflictException extends RuntimeException {

    private final String entityId;
    private final long serverRevision;
    private final List<String> conflictFields;
    private final Map<String, Object> serverSnapshot;
    private final Map<String, Object> attemptedChanges;

    public WorkItemConflictException(
            String entityId,
            long serverRevision,
            List<String> conflictFields,
            Map<String, Object> serverSnapshot,
            Map<String, Object> attemptedChanges
    ) {
        super("Work item was modified by another user.");
        this.entityId = entityId;
        this.serverRevision = serverRevision;
        this.conflictFields = conflictFields;
        this.serverSnapshot = serverSnapshot;
        this.attemptedChanges = attemptedChanges;
    }

    public String getEntityId() {
        return entityId;
    }

    public long getServerRevision() {
        return serverRevision;
    }

    public List<String> getConflictFields() {
        return conflictFields;
    }

    public Map<String, Object> getServerSnapshot() {
        return serverSnapshot;
    }

    public Map<String, Object> getAttemptedChanges() {
        return attemptedChanges;
    }
}
