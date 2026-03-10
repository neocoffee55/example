package com.taxworkbench.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkItemJpaRepository extends JpaRepository<WorkItemEntity, String> {
}
