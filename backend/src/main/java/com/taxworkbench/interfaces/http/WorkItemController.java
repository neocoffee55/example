package com.taxworkbench.interfaces.http;

import com.taxworkbench.application.WorkbenchDataService;
import com.taxworkbench.application.WorkbenchDataService.BulkInsertRequest;
import com.taxworkbench.application.WorkbenchDataService.BulkInsertResult;
import com.taxworkbench.application.WorkbenchDataService.WorkItemAuditView;
import com.taxworkbench.application.WorkbenchDataService.WorkItemPayload;
import com.taxworkbench.application.WorkbenchDataService.WorkItemView;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@CrossOrigin(origins = "http://localhost:8080")
@RequestMapping("/api/work-items")
class WorkItemController {

    private final WorkbenchDataService service;

    WorkItemController(WorkbenchDataService service) {
        this.service = service;
    }

    @GetMapping
    List<WorkItemView> find(
            @RequestParam(defaultValue = "") String client,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String assignee,
            @RequestParam(defaultValue = "") String dueDate
    ) {
        return service.findWorkItems(client, status, assignee, dueDate);
    }

    @GetMapping("/export")
    ResponseEntity<StreamingResponseBody> export(
            @RequestParam(defaultValue = "") String client,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String assignee,
            @RequestParam(defaultValue = "") String dueDate
    ) {
        StreamingResponseBody responseBody = outputStream -> {
            try {
                service.streamWorkItemsAsCsv(client, status, assignee, dueDate, outputStream);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tax-workbench-export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(responseBody);
    }

    @PostMapping
    WorkItemView create(@RequestBody WorkItemPayload payload) {
        return service.createWorkItem(payload);
    }

    @PostMapping("/bulk")
    BulkInsertResult bulkInsert(@RequestBody BulkInsertRequest request) {
        return service.bulkInsertWorkItems(request);
    }

    @PatchMapping("/{id}")
    WorkItemView update(@PathVariable String id, @RequestBody WorkItemPayload payload) {
        return service.updateWorkItem(id, payload);
    }

    @DeleteMapping("/{id}")
    void delete(
            @PathVariable String id,
            @RequestParam long revision,
            @RequestParam(defaultValue = "insu") String changedBy
    ) {
        service.deleteWorkItem(id, revision, changedBy);
    }

    @GetMapping("/{id}/audit-logs")
    List<WorkItemAuditView> auditLogs(@PathVariable String id) {
        return service.findWorkItemAuditLogs(id);
    }

    @GetMapping("/audit-logs")
    List<WorkItemAuditView> auditLogsByBizNo(@RequestParam(defaultValue = "") String bizNo) {
        return service.findWorkItemAuditLogsByBizNo(bizNo);
    }
}
