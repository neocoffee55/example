package com.taxworkbench.interfaces.http;

import com.taxworkbench.application.WorkbenchDataService;
import com.taxworkbench.application.WorkbenchDataService.ClientPayload;
import com.taxworkbench.application.WorkbenchDataService.ClientSaveRequest;
import com.taxworkbench.application.WorkbenchDataService.ClientView;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:8082")
@RequestMapping("/api/clients")
class ClientController {

    private final WorkbenchDataService service;

    ClientController(WorkbenchDataService service) {
        this.service = service;
    }

    @GetMapping
    List<ClientView> find(@RequestParam(defaultValue = "") String keyword) {
        return service.findClients(keyword);
    }

    @PutMapping
    List<ClientView> save(@RequestBody Object requestBody) {
        return service.saveClients(toClientSaveRequest(requestBody));
    }

    @SuppressWarnings("unchecked")
    private ClientSaveRequest toClientSaveRequest(Object requestBody) {
        if (requestBody instanceof List<?>) {
            return new ClientSaveRequest(toClientPayloads((List<?>) requestBody), List.of());
        }

        if (requestBody instanceof Map<?, ?>) {
            Map<String, Object> requestMap = (Map<String, Object>) requestBody;
            List<ClientPayload> items = toClientPayloads((List<?>) requestMap.getOrDefault("items", List.of()));
            List<String> deletedIds = (List<String>) requestMap.getOrDefault("deletedIds", List.of());
            return new ClientSaveRequest(items, deletedIds);
        }

        throw new IllegalArgumentException("법인정보 저장 요청 형식이 올바르지 않습니다.");
    }

    @SuppressWarnings("unchecked")
    private List<ClientPayload> toClientPayloads(List<?> items) {
        return items.stream()
                .map(item -> {
                    if (item instanceof ClientPayload payload) {
                        return payload;
                    }

                    Map<String, Object> row = (Map<String, Object>) item;
                    return new ClientPayload(
                            String.valueOf(row.getOrDefault("id", "")),
                            String.valueOf(row.getOrDefault("name", "")),
                            String.valueOf(row.getOrDefault("bizNo", "")),
                            String.valueOf(row.getOrDefault("type", "")),
                            String.valueOf(row.getOrDefault("status", "")),
                            String.valueOf(row.getOrDefault("tier", ""))
                    );
                })
                .toList();
    }
}
