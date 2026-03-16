package com.taxworkbench.application;

import com.taxworkbench.infrastructure.persistence.ClientEntity;
import com.taxworkbench.infrastructure.persistence.ClientJpaRepository;
import com.taxworkbench.infrastructure.persistence.WorkItemAuditLogEntity;
import com.taxworkbench.infrastructure.persistence.WorkItemAuditLogJpaRepository;
import com.taxworkbench.infrastructure.persistence.WorkItemEntity;
import com.taxworkbench.infrastructure.persistence.WorkItemJpaRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WorkbenchDataService {

    private static final String DEFAULT_CHANGED_BY = "insu";

    private final WorkItemJpaRepository workItemRepository;
    private final WorkItemAuditLogJpaRepository auditLogRepository;
    private final ClientJpaRepository clientRepository;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final int bulkInsertChunkSize;
    private final int bulkInsertMaxRequestSize;
    private final int exportBatchSize;

    public WorkbenchDataService(
            WorkItemJpaRepository workItemRepository,
            WorkItemAuditLogJpaRepository auditLogRepository,
            ClientJpaRepository clientRepository,
            PlatformTransactionManager transactionManager,
            @Value("${tax-workbench.bulk-insert.chunk-size:100}") int bulkInsertChunkSize,
            @Value("${tax-workbench.bulk-insert.max-request-size:1000}") int bulkInsertMaxRequestSize,
            @Value("${tax-workbench.export.batch-size:500}") int exportBatchSize
    ) {
        this.workItemRepository = workItemRepository;
        this.auditLogRepository = auditLogRepository;
        this.clientRepository = clientRepository;
        this.bulkInsertChunkSize = bulkInsertChunkSize;
        this.bulkInsertMaxRequestSize = bulkInsertMaxRequestSize;
        this.exportBatchSize = exportBatchSize;
        this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(readOnly = true)
    public List<WorkItemView> findWorkItems(String client, String status, String assignee, String dueDate) {
        return workItemRepository.search(
                        normalizeFilter(client),
                        normalizeFilter(status),
                        normalizeFilter(assignee),
                        parseOptionalDate(dueDate),
                        workItemSort()
                ).stream()
                .map(this::toWorkItemView)
                .toList();
    }

    @Transactional(readOnly = true)
    public void streamWorkItemsAsCsv(
            String client,
            String status,
            String assignee,
            String dueDate,
            OutputStream outputStream
    ) throws IOException {
        outputStream.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        outputStream.write("업체명,사업자번호,업무유형,상태,담당자,마감일,최근수정\n".getBytes(StandardCharsets.UTF_8));

        String normalizedClient = normalizeFilter(client);
        String normalizedStatus = normalizeFilter(status);
        String normalizedAssignee = normalizeFilter(assignee);
        LocalDate parsedDueDate = parseOptionalDate(dueDate);

        int pageNumber = 0;
        Page<WorkItemEntity> page;
        do {
            page = workItemRepository.search(
                    normalizedClient,
                    normalizedStatus,
                    normalizedAssignee,
                    parsedDueDate,
                    PageRequest.of(pageNumber, exportBatchSize, workItemSort())
            );

            for (WorkItemEntity item : page.getContent()) {
                String row = String.join(",",
                        csvEscape(item.getClientName()),
                        csvEscape(item.getBizNo()),
                        csvEscape(item.getWorkType()),
                        csvEscape(item.getStatus()),
                        csvEscape(item.getAssignee()),
                        csvEscape(item.getDueDate() == null ? "" : item.getDueDate().toString()),
                        csvEscape(formatInstant(item.getUpdatedAt()))
                ) + "\n";
                outputStream.write(row.getBytes(StandardCharsets.UTF_8));
            }
            outputStream.flush();
            pageNumber += 1;
        } while (page.hasNext());
    }

    private Sort workItemSort() {
        return Sort.by(
                Sort.Order.asc("clientName").ignoreCase(),
                Sort.Order.asc("workType").ignoreCase(),
                Sort.Order.asc("id")
        );
    }

    private String normalizeFilter(String value) {
        return value == null ? "" : value.trim();
    }

    private LocalDate parseOptionalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseDate(value);
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
        return toWorkItemView(saved);
    }

    public BulkInsertResult bulkInsertWorkItems(BulkInsertRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Bulk insert 요청에는 1건 이상의 WorkItem이 필요합니다.");
        }
        if (request.items().size() > bulkInsertMaxRequestSize) {
            throw new IllegalArgumentException(
                    "Bulk insert 최대 요청 건수를 초과했습니다. maxRequestSize=" + bulkInsertMaxRequestSize
            );
        }

        Set<String> seenRequestIds = new HashSet<>();
        Set<String> duplicatedRequestIds = new HashSet<>();
        List<String> requestedIds = request.items().stream()
                .map(WorkItemPayload::id)
                .filter(id -> id != null && !id.isBlank())
                .toList();

        request.items().forEach(item -> {
            String id = item.id();
            if (id == null || id.isBlank()) {
                return;
            }
            if (!seenRequestIds.add(id)) {
                duplicatedRequestIds.add(id);
            }
        });

        Set<String> existingIds = workItemRepository.findAllById(requestedIds).stream()
                .map(WorkItemEntity::getId)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        List<BulkInsertFailure> failures = new ArrayList<>();
        List<BulkInsertCandidate> chunkCandidates = new ArrayList<>();
        int successCount = 0;

        for (int index = 0; index < request.items().size(); index++) {
            WorkItemPayload payload = request.items().get(index);
            String validationError = validateBulkInsertPayload(payload, duplicatedRequestIds, existingIds);

            if (validationError != null) {
                failures.add(new BulkInsertFailure(index, payload.id(), validationError));
                continue;
            }

            try {
                chunkCandidates.add(new BulkInsertCandidate(index, toNewWorkItemEntity(payload)));
            } catch (IllegalArgumentException exception) {
                failures.add(new BulkInsertFailure(index, payload.id(), exception.getMessage()));
                continue;
            }

            if (chunkCandidates.size() == bulkInsertChunkSize) {
                successCount += persistBulkChunk(chunkCandidates, failures);
                chunkCandidates.clear();
            }
        }

        if (!chunkCandidates.isEmpty()) {
            successCount += persistBulkChunk(chunkCandidates, failures);
        }

        return new BulkInsertResult(
                request.items().size(),
                successCount,
                failures.size(),
                failures
        );
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
    public List<WorkItemAuditView> findWorkItemAuditLogsByBizNo(String bizNo) {
        String normalizedBizNo = normalizeFilter(bizNo);
        if (normalizedBizNo.isBlank()) {
            return List.of();
        }

        List<String> workItemIds = workItemRepository.findByBizNoOrderByUpdatedAtDescIdAsc(normalizedBizNo).stream()
                .map(WorkItemEntity::getId)
                .toList();

        if (workItemIds.isEmpty()) {
            return List.of();
        }

        return auditLogRepository.findByWorkItemIdInOrderByChangedAtDesc(workItemIds).stream()
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

    private String validateBulkInsertPayload(
            WorkItemPayload payload,
            Set<String> duplicatedRequestIds,
            Set<String> existingIds
    ) {
        if (payload == null) {
            return "요청 항목이 비어 있습니다.";
        }
        if (payload.id() == null || payload.id().isBlank()) {
            return "id는 필수입니다.";
        }
        if (duplicatedRequestIds.contains(payload.id())) {
            return "동일한 id가 요청 본문에 중복되어 있습니다.";
        }
        if (existingIds.contains(payload.id())) {
            return "이미 존재하는 WorkItem id입니다.";
        }
        if (payload.client() == null || payload.client().isBlank()) {
            return "client는 필수입니다.";
        }
        if (payload.bizNo() == null || payload.bizNo().isBlank()) {
            return "bizNo는 필수입니다.";
        }
        if (payload.workType() == null || payload.workType().isBlank()) {
            return "workType은 필수입니다.";
        }
        if (payload.status() == null || payload.status().isBlank()) {
            return "status는 필수입니다.";
        }
        if (payload.assignee() == null || payload.assignee().isBlank()) {
            return "assignee는 필수입니다.";
        }

        try {
            parseDate(payload.dueDate());
        } catch (RuntimeException exception) {
            return "dueDate 형식이 올바르지 않습니다. YYYY-MM-DD 형식을 사용해야 합니다.";
        }

        return null;
    }

    private WorkItemEntity toNewWorkItemEntity(WorkItemPayload payload) {
        return new WorkItemEntity(
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
    }

    private int persistBulkChunk(List<BulkInsertCandidate> candidates, List<BulkInsertFailure> failures) {
        try {
            requiresNewTransactionTemplate.executeWithoutResult(status ->
                    workItemRepository.saveAllAndFlush(candidates.stream().map(BulkInsertCandidate::entity).toList())
            );
            return candidates.size();
        } catch (RuntimeException chunkException) {
            int successCount = 0;
            for (BulkInsertCandidate candidate : candidates) {
                try {
                    requiresNewTransactionTemplate.executeWithoutResult(status ->
                            workItemRepository.saveAndFlush(candidate.entity())
                    );
                    successCount += 1;
                } catch (RuntimeException rowException) {
                    failures.add(new BulkInsertFailure(
                            candidate.index(),
                            candidate.entity().getId(),
                            normalizePersistenceError(rowException)
                    ));
                }
            }
            return successCount;
        }
    }

    private String normalizePersistenceError(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "저장 중 알 수 없는 오류가 발생했습니다.";
        }
        return message;
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

    private String csvEscape(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private String formatInstant(Instant value) {
        return value == null ? "" : value.toString();
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

    public record BulkInsertRequest(
            List<WorkItemPayload> items
    ) {
    }

    public record BulkInsertResult(
            int totalCount,
            int successCount,
            int failureCount,
            List<BulkInsertFailure> failures
    ) {
    }

    public record BulkInsertFailure(
            int index,
            String workItemId,
            String reason
    ) {
    }

    private record BulkInsertCandidate(
            int index,
            WorkItemEntity entity
    ) {
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
