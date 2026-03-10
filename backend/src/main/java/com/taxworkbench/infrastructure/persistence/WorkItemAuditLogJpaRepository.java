package com.taxworkbench.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkItemAuditLogJpaRepository extends JpaRepository<WorkItemAuditLogEntity, Long> {

    List<WorkItemAuditLogEntity> findByWorkItemIdOrderByChangedAtDesc(String workItemId);
}
