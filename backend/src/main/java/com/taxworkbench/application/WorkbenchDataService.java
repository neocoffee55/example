package com.taxworkbench.application;

import com.taxworkbench.infrastructure.persistence.ClientEntity;
import com.taxworkbench.infrastructure.persistence.ClientJpaRepository;
import com.taxworkbench.infrastructure.persistence.WorkItemAuditLogEntity;
import com.taxworkbench.infrastructure.persistence.WorkItemAuditLogJpaRepository;
import com.taxworkbench.infrastructure.persistence.WorkItemEntity;
import com.taxworkbench.infrastructure.persistence.WorkItemJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkbenchDataService {

    private static final String DEFAULT_CHANGED_BY = "insu";

    private final WorkItemJpaRepository workItemRepository;
    private final WorkItemAuditLogJpaRepository auditLogRepository;
    private final ClientJpaRepository clientRepository;

    public WorkbenchDataService(
            WorkItemJpaRepository workItemRepository,
            WorkItemAuditLogJpaRepository auditLogRepository,
            ClientJpaRepository clientRepository
    ) {
        this.workItemRepository = workItemRepository;
        this.auditLogRepository = auditLogRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkItemView> findWorkItems(String client, String status, String assignee, String dueDate) {
        return workItemRepository.findAll().stream()
                .filter(item -> client == null || client.isBlank() || item.getClientName().toLowerCase().contains(client.toLowerCase()))
                .filter(item -> status == null || status.isBlank() || item.getStatus().equalsIgnoreCase(status))
                .filter(item -> assignee == null || assignee.isBlank() || item.getAssignee().toLowerCase().contains(assignee.toLowerCase()))
                .filter(item -> dueDate == null || dueDate.isBlank() || (item.getDueDate() != null && item.getDueDate().toString().equals(dueDate)))
                .sorted(
                        Comparator.comparing(WorkItemEntity::getClientName, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(WorkItemEntity::getWorkType, String.CASE_INSENSITIVE_ORDER)
                )
                .map(this::toWorkItemView)
                .toList();
    }

    @Transactional
    public WorkItemView createWorkItem(WorkItemPayload payload) {
        WorkItemEntity entity = new WorkItemEntity(
                payload.id(),
                payload.client(),
                payload.bizNo(),
                payload.workType(),
                payload.status(),
                payload.assignee(),
                parseDate(payload.dueDate()),
                Instant.now(),
                0L
        );
        WorkItemEntity saved = workItemRepository.save(entity);
        recordAuditLogs(saved.getId(), saved.getRevision(), Map.of(), payload.toAuditMap(), payload.changedBy());
        return toWorkItemView(saved);
    }

    @Transactional
    public WorkItemView updateWorkItem(String id, WorkItemPayload payload) {
        WorkItemEntity entity = workItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found: " + id));

        if (payload.revision() != entity.getRevision()) {
            throw buildConflictException(entity, payload);
        }

        Map<String, Object> before = snapshot(entity);
        entity.update(
                payload.client(),
                payload.bizNo(),
                payload.workType(),
                payload.status(),
                payload.assignee(),
                parseDate(payload.dueDate()),
                Instant.now()
        );
        WorkItemEntity saved = workItemRepository.saveAndFlush(entity);
        recordAuditLogs(saved.getId(), saved.getRevision(), before, payload.toAuditMap(), payload.changedBy());
        return toWorkItemView(saved);
    }

    @Transactional
    public void deleteWorkItem(String id, long revision, String changedBy) {
        WorkItemEntity entity = workItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found: " + id));

        if (revision != entity.getRevision()) {
            throw buildConflictException(entity, new WorkItemPayload(
                    entity.getId(),
                    revision,
                    entity.getClientName(),
                    entity.getBizNo(),
                    entity.getWorkType(),
                    entity.getStatus(),
                    entity.getAssignee(),
                    entity.getDueDate() == null ? "" : entity.getDueDate().toString(),
                    changedBy
            ));
        }

        workItemRepository.delete(entity);
        recordAuditLogs(entity.getId(), entity.getRevision(), snapshot(entity), Map.of(), changedBy);
    }

    @Transactional(readOnly = true)
    public List<WorkItemAuditView> findWorkItemAuditLogs(String workItemId) {
        return auditLogRepository.findByWorkItemIdOrderByChangedAtDesc(workItemId).stream()
                .map(log -> new WorkItemAuditView(
                        log.getWorkItemId(),
                        log.getRevision(),
                        log.getChangedAt().toString(),
                        log.getChangedBy(),
                        log.getFieldName(),
                        log.getBeforeValue(),
                        log.getAfterValue()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClientView> findClients(String keyword) {
        List<ClientEntity> clients = keyword == null || keyword.isBlank()
                ? clientRepository.findAll()
                : clientRepository.findByNameContainingIgnoreCaseOrBizNoContainingIgnoreCase(keyword, keyword);

        return clients.stream()
                .sorted(Comparator.comparing(ClientEntity::getUpdatedAt).reversed())
                .map(client -> new ClientView(
                        client.getId(),
                        client.getName(),
                        client.getBizNo(),
                        client.getType(),
                        client.getStatus(),
                        client.getTier(),
                        client.getUpdatedAt().toString()
                ))
                .toList();
    }

    @Transactional
    public List<ClientView> saveClients(List<ClientPayload> payloads) {
        clientRepository.deleteAllInBatch();
        List<ClientEntity> entities = payloads.stream()
                .map(payload -> new ClientEntity(
                        payload.id(),
                        payload.name(),
                        payload.bizNo(),
                        payload.type(),
                        payload.status(),
                        payload.tier(),
                        Instant.now()
                ))
                .toList();
        clientRepository.saveAll(entities);
        return findClients("");
    }

    private WorkItemConflictException buildConflictException(WorkItemEntity entity, WorkItemPayload payload) {
        Map<String, Object> serverSnapshot = snapshot(entity);
        Map<String, Object> attemptedChanges = payload.toAuditMap();
        List<String> conflictFields = attemptedChanges.entrySet().stream()
                .filter(entry -> !String.valueOf(entry.getValue()).equals(String.valueOf(serverSnapshot.get(entry.getKey()))))
                .map(Map.Entry::getKey)
                .toList();

        return new WorkItemConflictException(
                entity.getId(),
                entity.getRevision(),
                conflictFields,
                serverSnapshot,
                attemptedChanges
        );
    }

    private void recordAuditLogs(
            String workItemId,
            long revision,
            Map<String, Object> before,
            Map<String, Object> after,
            String changedBy
    ) {
        List<WorkItemAuditLogEntity> logs = new ArrayList<>();
        for (String field : after.keySet()) {
            String beforeValue = stringify(before.get(field));
            String afterValue = stringify(after.get(field));
            if (!beforeValue.equals(afterValue)) {
                logs.add(new WorkItemAuditLogEntity(
                        workItemId,
                        revision,
                        Instant.now(),
                        changedBy == null || changedBy.isBlank() ? DEFAULT_CHANGED_BY : changedBy,
                        field,
                        beforeValue,
                        afterValue
                ));
            }
        }
        if (!before.isEmpty() && after.isEmpty()) {
            for (String field : before.keySet()) {
                logs.add(new WorkItemAuditLogEntity(
                        workItemId,
                        revision,
                        Instant.now(),
                        changedBy == null || changedBy.isBlank() ? DEFAULT_CHANGED_BY : changedBy,
                        field,
                        stringify(before.get(field)),
                        ""
                ));
            }
        }
        auditLogRepository.saveAll(logs);
    }

    private Map<String, Object> snapshot(WorkItemEntity entity) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("client", entity.getClientName());
        snapshot.put("bizNo", entity.getBizNo());
        snapshot.put("workType", entity.getWorkType());
        snapshot.put("status", entity.getStatus());
        snapshot.put("assignee", entity.getAssignee());
        snapshot.put("dueDate", entity.getDueDate() == null ? "" : entity.getDueDate().toString());
        return snapshot;
    }

    private WorkItemView toWorkItemView(WorkItemEntity item) {
        return new WorkItemView(
                item.getId(),
                item.getRevision(),
                item.getClientName(),
                item.getBizNo(),
                item.getWorkType(),
                item.getStatus(),
                item.getAssignee(),
                item.getDueDate() == null ? "" : item.getDueDate().toString(),
                item.getUpdatedAt().toString()
        );
    }

    private LocalDate parseDate(String dueDate) {
        return dueDate == null || dueDate.isBlank() ? null : LocalDate.parse(dueDate);
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public record WorkItemView(
            String id,
            long revision,
            String client,
            String bizNo,
            String workType,
            String status,
            String assignee,
            String dueDate,
            String updatedAt
    ) {
    }

    public record WorkItemPayload(
            String id,
            long revision,
            String client,
            String bizNo,
            String workType,
            String status,
            String assignee,
            String dueDate,
            String changedBy
    ) {
        public Map<String, Object> toAuditMap() {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("client", client);
            snapshot.put("bizNo", bizNo);
            snapshot.put("workType", workType);
            snapshot.put("status", status);
            snapshot.put("assignee", assignee);
            snapshot.put("dueDate", dueDate == null ? "" : dueDate);
            return snapshot;
        }
    }

    public record WorkItemAuditView(
            String workItemId,
            long revision,
            String changedAt,
            String changedBy,
            String fieldName,
            String beforeValue,
            String afterValue
    ) {
    }

    public record ClientView(
            String id,
            String name,
            String bizNo,
            String type,
            String status,
            String tier,
            String updatedAt
    ) {
    }

    public record ClientPayload(
            String id,
            String name,
            String bizNo,
            String type,
            String status,
            String tier
    ) {
    }
}
