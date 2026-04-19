package com.taxworkbench.interfaces.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.taxworkbench.application.shared.CursorPage;
import com.taxworkbench.application.workitem.*;
import com.taxworkbench.domain.shared.ClientTier;
import com.taxworkbench.domain.shared.ClientType;
import com.taxworkbench.domain.shared.WorkItemStatus;
import com.taxworkbench.domain.shared.WorkItemType;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/work-items")
public class WorkItemController {

    private final WorkItemQueryUseCase workItemQueryUseCase;
    private final WorkItemCommandUseCase workItemCommandUseCase;

    public WorkItemController(
            WorkItemQueryUseCase workItemQueryUseCase,
            WorkItemCommandUseCase workItemCommandUseCase
    ) {
        this.workItemQueryUseCase = workItemQueryUseCase;
        this.workItemCommandUseCase = workItemCommandUseCase;
    }

    @GetMapping
    CursorPage<WorkItemView> list(
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) String statuses,
            @RequestParam(required = false) String assignees,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo,
            @RequestParam(required = false) ClientType clientType,
            @RequestParam(required = false) ClientTier clientTier,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int pageSize,
            @RequestParam(required = false) String cursor
    ) {
        return workItemQueryUseCase.listWorkItems(new WorkItemListQuery(
                clientName,
                parseStatuses(statuses),
                parseStringList(assignees),
                dueDateFrom,
                dueDateTo,
                clientType,
                clientTier,
                sort,
                pageSize,
                cursor
        ));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    WorkItemView create(@Valid @RequestBody CreateWorkItemRequest request) {
        return workItemCommandUseCase.createWorkItem(new CreateWorkItemCommand(
                request.clientId(),
                request.type(),
                request.status(),
                request.assignee(),
                request.dueDate(),
                request.tags(),
                request.memo()
        ));
    }

    @PatchMapping("/{id}")
    WorkItemView patch(@PathVariable Long id, @Valid @RequestBody PatchWorkItemRequest request) {
        return workItemCommandUseCase.patchWorkItem(new PatchWorkItemCommand(
                id,
                request.version(),
                request.operations()
                        .stream()
                        .map(operation -> new PatchWorkItemCommand.PatchOperation(operation.field(), operation.value(), operation.baseValue()))
                        .toList()
        ));
    }

    @GetMapping("/{id}/audit-logs")
    CursorPage<AuditLogView> auditLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int pageSize,
            @RequestParam(required = false) String cursor
    ) {
        return workItemQueryUseCase.listAuditLogs(new AuditLogQuery(id, pageSize, cursor));
    }

    @PostMapping("/bulk-import")
    BulkImportResult bulkImport(@Valid @RequestBody BulkImportRequest request) {
        return workItemCommandUseCase.bulkImport(new BulkImportWorkItemsCommand(
                request.requestId(),
                request.items()
                        .stream()
                        .map(item -> new BulkImportWorkItemsCommand.BulkImportItem(
                                item.clientId(),
                                item.type(),
                                item.status(),
                                item.assignee(),
                                item.dueDate(),
                                item.tags(),
                                item.memo()
                        ))
                        .toList()
        ));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    void export(
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) String statuses,
            @RequestParam(required = false) String assignees,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo,
            @RequestParam(required = false) ClientType clientType,
            @RequestParam(required = false) ClientTier clientTier,
            @RequestParam(required = false) String sort,
            HttpServletResponse response
    ) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"work-items-export.csv\"");
        workItemCommandUseCase.exportWorkItems(new WorkItemListQuery(
                clientName,
                parseStatuses(statuses),
                parseStringList(assignees),
                dueDateFrom,
                dueDateTo,
                clientType,
                clientTier,
                sort,
                0,
                null
        ), response.getOutputStream());
    }

    private static List<WorkItemStatus> parseStatuses(String raw) {
        return raw == null || raw.isBlank()
                ? List.of()
                : Arrays.stream(raw.split(",")).map(String::trim).filter(token -> !token.isEmpty()).map(WorkItemStatus::valueOf).toList();
    }

    private static List<String> parseStringList(String raw) {
        return raw == null || raw.isBlank()
                ? List.of()
                : Arrays.stream(raw.split(",")).map(String::trim).filter(token -> !token.isEmpty()).toList();
    }

    public record CreateWorkItemRequest(
            @NotNull Long clientId,
            @NotNull WorkItemType type,
            @NotNull WorkItemStatus status,
            String assignee,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            List<String> tags,
            String memo
    ) {
        public CreateWorkItemRequest {
            tags = tags == null ? List.of() : List.copyOf(tags);
        }
    }

    public record PatchWorkItemRequest(
            @Positive long version,
            @NotEmpty List<PatchOperationRequest> operations
    ) {
    }

    public record PatchOperationRequest(
            @NotBlank String field,
            @NotNull JsonNode value,
            JsonNode baseValue
    ) {
    }

    public record BulkImportRequest(
            @NotBlank String requestId,
            @NotEmpty List<BulkImportItemRequest> items
    ) {
    }

    public record BulkImportItemRequest(
            @NotNull Long clientId,
            @NotNull WorkItemType type,
            @NotNull WorkItemStatus status,
            String assignee,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            List<String> tags,
            String memo
    ) {
        public BulkImportItemRequest {
            tags = tags == null ? List.of() : List.copyOf(tags);
        }
    }
}
