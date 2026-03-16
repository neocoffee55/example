package com.taxworkbench.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkItemJpaRepository extends JpaRepository<WorkItemEntity, String> {

    @Query("""
            select w
            from WorkItemEntity w
            where (:client = '' or lower(w.clientName) like lower(concat('%', :client, '%')))
              and (:status = '' or upper(w.status) = upper(:status))
              and (:assignee = '' or lower(w.assignee) like lower(concat('%', :assignee, '%')))
              and (:dueDate is null or w.dueDate = :dueDate)
            """)
    List<WorkItemEntity> search(
            @Param("client") String client,
            @Param("status") String status,
            @Param("assignee") String assignee,
            @Param("dueDate") LocalDate dueDate,
            Sort sort
    );

    @Query("""
            select w
            from WorkItemEntity w
            where (:client = '' or lower(w.clientName) like lower(concat('%', :client, '%')))
              and (:status = '' or upper(w.status) = upper(:status))
              and (:assignee = '' or lower(w.assignee) like lower(concat('%', :assignee, '%')))
              and (:dueDate is null or w.dueDate = :dueDate)
            """)
    Page<WorkItemEntity> search(
            @Param("client") String client,
            @Param("status") String status,
            @Param("assignee") String assignee,
            @Param("dueDate") LocalDate dueDate,
            Pageable pageable
    );

    List<WorkItemEntity> findByBizNoOrderByUpdatedAtDescIdAsc(String bizNo);

    Optional<WorkItemEntity> findFirstByBizNo(String bizNo);
}
