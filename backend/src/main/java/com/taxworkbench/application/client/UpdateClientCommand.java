package com.taxworkbench.application.client;

import com.taxworkbench.domain.shared.ClientStatus;
import com.taxworkbench.domain.shared.ClientTier;

public record UpdateClientCommand(
        Long clientId,
        long version,
        ClientStatus status,
        ClientTier tier
) {
}
