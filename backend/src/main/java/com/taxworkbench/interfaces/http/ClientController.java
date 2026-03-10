package com.taxworkbench.interfaces.http;

import com.taxworkbench.application.WorkbenchDataService;
import com.taxworkbench.application.WorkbenchDataService.ClientPayload;
import com.taxworkbench.application.WorkbenchDataService.ClientView;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:8080")
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
    List<ClientView> save(@RequestBody List<ClientPayload> payloads) {
        return service.saveClients(payloads);
    }
}
