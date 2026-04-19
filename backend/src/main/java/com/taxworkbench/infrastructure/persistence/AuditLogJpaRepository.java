package com.taxworkbench.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, Long> {

    Page<AuditLogEntity> findByWorkItemIdOrderByChangedAtDescIdDesc(Long workItemId, Pageable pageable);

    List<AuditLogEntity> findByWorkItemIdOrderByChangedAtDescIdDesc(Long workItemId);
}
