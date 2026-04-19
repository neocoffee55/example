package com.taxworkbench.application.client;

import com.taxworkbench.domain.shared.ClientStatus;
import com.taxworkbench.domain.shared.ClientTier;
import com.taxworkbench.domain.shared.ClientType;

import java.time.Instant;

public record ClientView(
        Long id,
        String name,
        String bizNo,
        ClientType type,
        ClientStatus status,
        ClientTier tier,
        long version,
        Instant updatedAt
) {
}
