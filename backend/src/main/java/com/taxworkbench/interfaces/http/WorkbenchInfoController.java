package com.taxworkbench.interfaces.http;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class WorkbenchInfoController {

    @GetMapping("/info")
    Map<String, Object> info() {
        return Map.of(
                "name", "Tax Workbench",
                "timestamp", Instant.now().toString(),
                "boundedContexts", List.of("domain", "application", "infrastructure", "interfaces"),
                "plannedCapabilities", List.of("listing", "inline-edit", "audit-log", "bulk-insert", "csv-export"));
    }
}
