package com.taxworkbench.interfaces.http;

import com.taxworkbench.application.client.*;
import com.taxworkbench.application.shared.CursorPage;
import com.taxworkbench.domain.shared.ClientStatus;
import com.taxworkbench.domain.shared.ClientTier;
import com.taxworkbench.domain.shared.ClientType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientQueryUseCase clientQueryUseCase;
    private final ClientCommandUseCase clientCommandUseCase;

    public ClientController(
            ClientQueryUseCase clientQueryUseCase,
            ClientCommandUseCase clientCommandUseCase
    ) {
        this.clientQueryUseCase = clientQueryUseCase;
        this.clientCommandUseCase = clientCommandUseCase;
    }

    @GetMapping
    CursorPage<ClientView> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ClientStatus status,
            @RequestParam(required = false) ClientType type,
            @RequestParam(required = false) ClientTier tier,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int pageSize,
            @RequestParam(required = false) String cursor
    ) {
        return clientQueryUseCase.listClients(new ClientListQuery(name, status, type, tier, pageSize, cursor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClientView create(@Valid @RequestBody CreateClientRequest request) {
        return clientCommandUseCase.createClient(new CreateClientCommand(
                request.name(),
                request.bizNo(),
                request.type(),
                request.status(),
                request.tier()
        ));
    }

    @PatchMapping("/{id}")
    ClientView patch(@PathVariable Long id, @Valid @RequestBody UpdateClientRequest request) {
        return clientCommandUseCase.updateClient(new UpdateClientCommand(id, request.version(), request.status(), request.tier()));
    }

    public record CreateClientRequest(
            @NotBlank String name,
            @NotBlank String bizNo,
            @NotNull ClientType type,
            @NotNull ClientStatus status,
            @NotNull ClientTier tier
    ) {
    }

    public record UpdateClientRequest(
            @Positive long version,
            ClientStatus status,
            ClientTier tier
    ) {
    }
}
