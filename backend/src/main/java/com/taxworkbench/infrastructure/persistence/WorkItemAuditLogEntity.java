package com.taxworkbench.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "work_item_audit_log")
public class WorkItemAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_item_id", nullable = false)
    private String workItemId;

    @Column(nullable = false)
    private long revision;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "before_value")
    private String beforeValue;

    @Column(name = "after_value")
    private String afterValue;

    protected WorkItemAuditLogEntity() {
    }

    public WorkItemAuditLogEntity(
            String workItemId,
            long revision,
            Instant changedAt,
            String changedBy,
            String fieldName,
            String beforeValue,
            String afterValue
    ) {
        this.workItemId = workItemId;
        this.revision = revision;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
        this.fieldName = fieldName;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
    }

    public String getWorkItemId() {
        return workItemId;
    }

    public long getRevision() {
        return revision;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getBeforeValue() {
        return beforeValue;
    }

    public String getAfterValue() {
        return afterValue;
    }
}
