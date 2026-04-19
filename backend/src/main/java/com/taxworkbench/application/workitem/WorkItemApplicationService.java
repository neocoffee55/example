package com.taxworkbench.application.workitem;

import com.fasterxml.jackson.databind.JsonNode;
import com.taxworkbench.application.error.ApiErrorDetail;
import com.taxworkbench.application.error.ConflictException;
import com.taxworkbench.application.error.InvalidRequestException;
import com.taxworkbench.application.error.NotFoundException;
import com.taxworkbench.application.error.PolicyViolationException;
import com.taxworkbench.application.shared.CursorCodec;
import com.taxworkbench.application.shared.CursorPage;
import com.taxworkbench.domain.shared.AuditSource;
import com.taxworkbench.domain.shared.ClientStatus;
import com.taxworkbench.domain.shared.ClientTier;
import com.taxworkbench.domain.shared.ClientType;
import com.taxworkbench.domain.shared.WorkItemStatus;
import com.taxworkbench.domain.shared.WorkItemType;
import com.taxworkbench.infrastructure.persistence.AuditLogEntity;
import com.taxworkbench.infrastructure.persistence.AuditLogJpaRepository;
import com.taxworkbench.infrastructure.persistence.ClientEntity;
import com.taxworkbench.infrastructure.persistence.ClientJpaRepository;
import com.taxworkbench.infrastructure.persistence.WorkItemEntity;
import com.taxworkbench.infrastructure.persistence.WorkItemJpaRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class WorkItemApplicationService implements WorkItemQueryUseCase, WorkItemCommandUseCase {

    private final WorkItemJpaRepository workItemJpaRepository;
    private final ClientJpaRepository clientJpaRepository;
    private final AuditLogJpaRepository auditLogJpaRepository;

    public WorkItemApplicationService(
            WorkItemJpaRepository workItemJpaRepository,
            ClientJpaRepository clientJpaRepository,
            AuditLogJpaRepository auditLogJpaRepository
    ) {
        this.workItemJpaRepository = workItemJpaRepository;
        this.clientJpaRepository = clientJpaRepository;
        this.auditLogJpaRepository = auditLogJpaRepository;
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public CursorPage<WorkItemView> listWorkItems(WorkItemListQuery query) {
        int pageSize = normalizePageSize(query.pageSize());
        WorkItemSort parsedSort = parseSort(query.sort());
        if (query.cursor() != null && !query.cursor().isBlank() && !parsedSort.cursorCompatible()) {
            throw new InvalidRequestException("Cursor pagination currently supports only the default sort order.", List.of(new ApiErrorDetail("sort", "cursor_requires_default_sort")));
        }
        var page = workItemJpaRepository.findAll(
                specification(query).and(afterWorkItemCursor(query.cursor())),
                PageRequest.of(0, pageSize + 1, parsedSort.sort())
        );
        boolean hasNext = page.getContent().size() > pageSize;
        List<WorkItemEntity> content = hasNext ? page.getContent().subList(0, pageSize) : page.getContent();
        String nextCursor = hasNext && !content.isEmpty()
                ? CursorCodec.encode(List.of(
                        String.valueOf(content.get(content.size() - 1).getDueDate()),
                        content.get(content.size() - 1).getClient().getName(),
                        String.valueOf(content.get(content.size() - 1).getId())
                ))
                : null;
        return new CursorPage<>(content.stream().map(this::toView).toList(), new CursorPage.Page(nextCursor, pageSize, hasNext));
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public CursorPage<AuditLogView> listAuditLogs(AuditLogQuery query) {
        workItemJpaRepository.findById(query.workItemId())
                .orElseThrow(() -> new NotFoundException("WORK_ITEM_NOT_FOUND", "Work item %s does not exist.".formatted(query.workItemId())));
        int pageSize = normalizePageSize(query.pageSize());
        List<String> cursorParts = CursorCodec.decode(query.cursor());
        List<AuditLogEntity> filtered = auditLogJpaRepository.findByWorkItemIdOrderByChangedAtDescIdDesc(query.workItemId()).stream()
                .filter(entity -> isAfterAuditCursor(entity, cursorParts))
                .limit(pageSize + 1L)
                .toList();
        boolean hasNext = filtered.size() > pageSize;
        List<AuditLogEntity> content = hasNext ? filtered.subList(0, pageSize) : filtered;
        String nextCursor = hasNext && !content.isEmpty()
                ? CursorCodec.encode(List.of(String.valueOf(content.get(content.size() - 1).getChangedAt()), String.valueOf(content.get(content.size() - 1).getId())))
                : null;
        return new CursorPage<>(content.stream().map(this::toAuditView).toList(), new CursorPage.Page(nextCursor, pageSize, hasNext));
    }

    @Override
    public WorkItemView createWorkItem(CreateWorkItemCommand command) {
        ClientEntity client = requireClient(command.clientId());
        validateClientPolicy(client, command.assignee(), command.type());
        WorkItemEntity entity = new WorkItemEntity();
        entity.setClient(client);
        entity.setType(command.type());
        entity.setStatus(command.status());
        entity.setAssignee(command.assignee());
        entity.setDueDate(command.dueDate());
        entity.setTags(new ArrayList<>(command.tags() == null ? List.of() : command.tags()));
        entity.setMemo(command.memo());
        WorkItemEntity saved = workItemJpaRepository.save(entity);
        appendAudit(saved.getId(), "status", null, String.valueOf(saved.getStatus()), AuditSource.CREATE);
        return toView(saved);
    }

    @Override
    public WorkItemView patchWorkItem(PatchWorkItemCommand command) {
        WorkItemEntity entity = workItemJpaRepository.findById(command.workItemId())
                .orElseThrow(() -> new NotFoundException("WORK_ITEM_NOT_FOUND", "Work item %s does not exist.".formatted(command.workItemId())));
        if (entity.getVersion() != command.version()) {
            List<ConflictException.FieldConflict> fieldConflicts = command.operations().stream()
                    .map(operation -> new ConflictException.FieldConflict(
                            operation.field(),
                            stringify(operation.baseValue()),
                            stringify(operation.value()),
                            currentFieldValue(entity, operation.field())
                    ))
                    .toList();
            throw new ConflictException(
                    "WORK_ITEM_CONFLICT",
                    "The work item was modified by another user.",
                    entity.getId(),
                    entity.getVersion(),
                    "system",
                    entity.getUpdatedAt(),
                    fieldConflicts
            );
        }
        if (command.operations() == null || command.operations().isEmpty()) {
            throw new InvalidRequestException("At least one patch operation is required.", List.of());
        }
        List<AuditChange> auditChanges = new ArrayList<>();
        for (PatchWorkItemCommand.PatchOperation operation : command.operations()) {
            applyPatch(entity, operation, auditChanges);
        }
        validateClientPolicy(entity.getClient(), entity.getAssignee(), entity.getType());
        WorkItemEntity saved = workItemJpaRepository.save(entity);
        auditChanges.forEach(change -> appendAudit(saved.getId(), change.field(), change.beforeValue(), change.afterValue(), AuditSource.INLINE_EDIT));
        return toView(saved);
    }

    @Override
    public BulkImportResult bulkImport(BulkImportWorkItemsCommand command) {
        List<BulkImportResult.Failure> failures = new ArrayList<>();
        int created = 0;
        List<BulkImportWorkItemsCommand.BulkImportItem> items = command.items() == null ? List.of() : command.items();
        for (int i = 0; i < items.size(); i++) {
            BulkImportWorkItemsCommand.BulkImportItem item = items.get(i);
            try {
                createWorkItem(new CreateWorkItemCommand(
                        item.clientId(),
                        item.type(),
                        item.status(),
                        item.assignee(),
                        item.dueDate(),
                        item.tags(),
                        item.memo()
                ));
                created++;
            } catch (NotFoundException | PolicyViolationException | InvalidRequestException exception) {
                failures.add(new BulkImportResult.Failure(i + 1, exception.code(), exception.getMessage()));
            }
        }
        return new BulkImportResult(
                command.requestId(),
                new BulkImportResult.Summary(items.size(), created, failures.size()),
                failures
        );
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public void exportWorkItems(WorkItemListQuery query, OutputStream outputStream) {
        WorkItemSort parsedSort = parseSort(query.sort());
        try {
            Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            writer.write("id,clientName,bizNo,type,status,assignee,dueDate,tags,memo,updatedAt,clientType,clientTier,clientStatus\n");

            int pageNumber = 0;
            while (true) {
                var page = workItemJpaRepository.findAll(
                        specification(query),
                        PageRequest.of(pageNumber, 500, parsedSort.sort())
                );
                for (WorkItemEntity entity : page.getContent()) {
                    writeCsvLine(writer, toView(entity));
                }
                writer.flush();
                if (!page.hasNext()) {
                    break;
                }
                pageNumber++;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to export CSV.", exception);
        }
    }

    private void applyPatch(WorkItemEntity entity, PatchWorkItemCommand.PatchOperation operation, List<AuditChange> auditChanges) {
        String field = operation.field();
        JsonNode value = operation.value();
        switch (field) {
            case "status" -> {
                WorkItemStatus nextStatus = WorkItemStatus.valueOf(value.asText());
                if (entity.getStatus() != nextStatus) {
                    auditChanges.add(new AuditChange("status", String.valueOf(entity.getStatus()), String.valueOf(nextStatus)));
                    entity.setStatus(nextStatus);
                }
            }
            case "dueDate" -> {
                LocalDate nextDueDate = LocalDate.parse(value.asText());
                if (!entity.getDueDate().equals(nextDueDate)) {
                    auditChanges.add(new AuditChange("dueDate", String.valueOf(entity.getDueDate()), String.valueOf(nextDueDate)));
                    entity.setDueDate(nextDueDate);
                }
            }
            case "assignee" -> {
                String nextAssignee = value.isNull() ? null : value.asText();
                if (!safeEquals(entity.getAssignee(), nextAssignee)) {
                    auditChanges.add(new AuditChange("assignee", entity.getAssignee(), nextAssignee));
                    entity.setAssignee(nextAssignee);
                }
            }
            case "memo" -> {
                String nextMemo = value.isNull() ? null : value.asText();
                if (!safeEquals(entity.getMemo(), nextMemo)) {
                    auditChanges.add(new AuditChange("memo", entity.getMemo(), nextMemo));
                    entity.setMemo(nextMemo);
                }
            }
            case "tags" -> {
                List<String> nextTags = new ArrayList<>();
                value.forEach(node -> nextTags.add(node.asText()));
                if (!entity.getTags().equals(nextTags)) {
                    auditChanges.add(new AuditChange("tags", String.join("|", entity.getTags()), String.join("|", nextTags)));
                    entity.setTags(nextTags);
                }
            }
            default -> throw new InvalidRequestException("Unknown patch field.", List.of(new ApiErrorDetail("field", field)));
        }
    }

    private void validateClientPolicy(ClientEntity client, String assignee, WorkItemType workItemType) {
        if (client.getStatus() == ClientStatus.INACTIVE) {
            throw new PolicyViolationException("Inactive clients cannot receive new work items.", List.of(new ApiErrorDetail("clientId", "inactive_client")));
        }
        if (client.getTier() == ClientTier.VIP && (assignee == null || assignee.isBlank())) {
            throw new PolicyViolationException("VIP client work items require an assignee.", List.of(new ApiErrorDetail("assignee", "required_for_vip_client")));
        }
        if (client.getType() == ClientType.CORPORATE && workItemType == WorkItemType.ETC) {
            throw new PolicyViolationException("Corporate clients require an explicit supported work item type.", List.of(new ApiErrorDetail("type", "unsupported_for_client_type")));
        }
    }

    private ClientEntity requireClient(Long clientId) {
        return clientJpaRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client %s does not exist.".formatted(clientId)));
    }

    private Specification<WorkItemEntity> specification(WorkItemListQuery query) {
        return Specification.where(clientNameContains(query.clientName()))
                .and(inStatuses(query.statuses()))
                .and(inAssignees(query.assignees()))
                .and(dueDateFrom(query.dueDateFrom()))
                .and(dueDateTo(query.dueDateTo()))
                .and(equalsClientType(query.clientType()))
                .and(equalsClientTier(query.clientTier()));
    }

    private Specification<WorkItemEntity> afterWorkItemCursor(String cursor) {
        List<String> parts = CursorCodec.decode(cursor);
        if (parts.isEmpty()) {
            return (root, ignoredQuery, criteriaBuilder) -> criteriaBuilder.conjunction();
        }
        LocalDate dueDate = LocalDate.parse(parts.get(0));
        String clientName = parts.get(1);
        Long id = Long.parseLong(parts.get(2));
        return (root, ignoredQuery, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.greaterThan(root.get("dueDate"), dueDate),
                criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("dueDate"), dueDate),
                        criteriaBuilder.or(
                                criteriaBuilder.greaterThan(root.get("client").get("name"), clientName),
                                criteriaBuilder.and(
                                        criteriaBuilder.equal(root.get("client").get("name"), clientName),
                                        criteriaBuilder.greaterThan(root.get("id"), id)
                                )
                        )
                )
        );
    }

    private Specification<WorkItemEntity> clientNameContains(String clientName) {
        return (root, ignoredQuery, criteriaBuilder) ->
                clientName == null || clientName.isBlank()
                        ? criteriaBuilder.conjunction()
                        : criteriaBuilder.like(criteriaBuilder.lower(root.get("client").get("name")), "%" + clientName.toLowerCase() + "%");
    }

    private Specification<WorkItemEntity> inStatuses(List<WorkItemStatus> statuses) {
        return (root, ignoredQuery, criteriaBuilder) ->
                statuses == null || statuses.isEmpty()
                        ? criteriaBuilder.conjunction()
                        : root.get("status").in(statuses);
    }

    private Specification<WorkItemEntity> inAssignees(List<String> assignees) {
        return (root, ignoredQuery, criteriaBuilder) ->
                assignees == null || assignees.isEmpty()
                        ? criteriaBuilder.conjunction()
                        : root.get("assignee").in(assignees);
    }

    private Specification<WorkItemEntity> dueDateFrom(LocalDate dueDateFrom) {
        return (root, ignoredQuery, criteriaBuilder) ->
                dueDateFrom == null ? criteriaBuilder.conjunction() : criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), dueDateFrom);
    }

    private Specification<WorkItemEntity> dueDateTo(LocalDate dueDateTo) {
        return (root, ignoredQuery, criteriaBuilder) ->
                dueDateTo == null ? criteriaBuilder.conjunction() : criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), dueDateTo);
    }

    private Specification<WorkItemEntity> equalsClientType(ClientType clientType) {
        return (root, ignoredQuery, criteriaBuilder) ->
                clientType == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("client").get("type"), clientType);
    }

    private Specification<WorkItemEntity> equalsClientTier(ClientTier clientTier) {
        return (root, ignoredQuery, criteriaBuilder) ->
                clientTier == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("client").get("tier"), clientTier);
    }

    private void appendAudit(Long workItemId, String field, String before, String after, AuditSource source) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setWorkItemId(workItemId);
        entity.setFieldName(field);
        entity.setBeforeValue(before);
        entity.setAfterValue(after);
        entity.setActorId("system");
        entity.setActorName("system");
        entity.setSource(source);
        auditLogJpaRepository.save(entity);
    }

    private WorkItemView toView(WorkItemEntity entity) {
        ClientEntity client = entity.getClient();
        return new WorkItemView(
                entity.getId(),
                client.getId(),
                client.getName(),
                client.getBizNo(),
                entity.getType(),
                entity.getStatus(),
                entity.getAssignee(),
                entity.getDueDate(),
                List.copyOf(entity.getTags()),
                entity.getMemo(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                client.getType(),
                client.getTier(),
                client.getStatus()
        );
    }

    private AuditLogView toAuditView(AuditLogEntity entity) {
        return new AuditLogView(
                entity.getId(),
                entity.getWorkItemId(),
                entity.getFieldName(),
                entity.getBeforeValue(),
                entity.getAfterValue(),
                entity.getActorId(),
                entity.getActorName(),
                entity.getSource(),
                entity.getChangedAt()
        );
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return 50;
        }
        return Math.min(pageSize, 200);
    }

    private boolean safeEquals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private boolean isAfterAuditCursor(AuditLogEntity entity, List<String> cursorParts) {
        if (cursorParts.isEmpty()) {
            return true;
        }
        String changedAt = cursorParts.get(0);
        Long id = Long.parseLong(cursorParts.get(1));
        int timeCompare = entity.getChangedAt().compareTo(java.time.Instant.parse(changedAt));
        if (timeCompare < 0) {
            return true;
        }
        return timeCompare == 0 && entity.getId() < id;
    }

    private String currentFieldValue(WorkItemEntity entity, String field) {
        return switch (field) {
            case "status" -> String.valueOf(entity.getStatus());
            case "dueDate" -> String.valueOf(entity.getDueDate());
            case "assignee" -> entity.getAssignee();
            case "memo" -> entity.getMemo();
            case "tags" -> String.join("|", entity.getTags());
            default -> null;
        };
    }

    private String stringify(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            node.forEach(item -> parts.add(item.asText()));
            return String.join("|", parts);
        }
        return node.toString();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private WorkItemSort parseSort(String rawSort) {
        try {
            return WorkItemSort.parse(rawSort);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(exception.getMessage(), List.of(new ApiErrorDetail("sort", "invalid_sort")));
        }
    }

    private void writeCsvLine(Writer writer, WorkItemView item) throws IOException {
        writer.write(String.valueOf(item.id()));
        writer.write(',');
        writer.write(csv(item.clientName()));
        writer.write(',');
        writer.write(csv(item.bizNo()));
        writer.write(',');
        writer.write(String.valueOf(item.type()));
        writer.write(',');
        writer.write(String.valueOf(item.status()));
        writer.write(',');
        writer.write(csv(item.assignee()));
        writer.write(',');
        writer.write(String.valueOf(item.dueDate()));
        writer.write(',');
        writer.write(csv(String.join("|", item.tags())));
        writer.write(',');
        writer.write(csv(item.memo()));
        writer.write(',');
        writer.write(String.valueOf(item.updatedAt()));
        writer.write(',');
        writer.write(String.valueOf(item.clientType()));
        writer.write(',');
        writer.write(String.valueOf(item.clientTier()));
        writer.write(',');
        writer.write(String.valueOf(item.clientStatus()));
        writer.write('\n');
    }

    private record AuditChange(
            String field,
            String beforeValue,
            String afterValue
    ) {
    }
}
