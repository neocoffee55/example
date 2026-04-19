package com.taxworkbench.application.client;

import com.taxworkbench.domain.shared.ClientStatus;
import com.taxworkbench.domain.shared.ClientTier;
import com.taxworkbench.domain.shared.ClientType;

public record CreateClientCommand(
        String name,
        String bizNo,
        ClientType type,
        ClientStatus status,
        ClientTier tier
) {
}
