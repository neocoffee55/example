package com.taxworkbench.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "work_item")
public class WorkItemEntity {

    @Id
    private String id;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "biz_no", nullable = false)
    private String bizNo;

    @Column(name = "work_type", nullable = false)
    private String workType;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String assignee;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long revision;

    protected WorkItemEntity() {
    }

    public WorkItemEntity(
            String id,
            String clientName,
            String bizNo,
            String workType,
            String status,
            String assignee,
            LocalDate dueDate,
            Instant updatedAt,
            long revision
    ) {
        this.id = id;
        this.clientName = clientName;
        this.bizNo = bizNo;
        this.workType = workType;
        this.status = status;
        this.assignee = assignee;
        this.dueDate = dueDate;
        this.updatedAt = updatedAt;
        this.revision = revision;
    }

    public String getId() {
        return id;
    }

    public String getClientName() {
        return clientName;
    }

    public String getBizNo() {
        return bizNo;
    }

    public String getWorkType() {
        return workType;
    }

    public String getStatus() {
        return status;
    }

    public String getAssignee() {
        return assignee;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getRevision() {
        return revision;
    }

    public void update(String clientName, String bizNo, String workType, String status, String assignee, LocalDate dueDate, Instant updatedAt) {
        this.clientName = clientName;
        this.bizNo = bizNo;
        this.workType = workType;
        this.status = status;
        this.assignee = assignee;
        this.dueDate = dueDate;
        this.updatedAt = updatedAt;
    }
}
