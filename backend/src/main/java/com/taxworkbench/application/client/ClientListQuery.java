package com.taxworkbench.application.client;

import com.taxworkbench.domain.shared.ClientStatus;
import com.taxworkbench.domain.shared.ClientTier;
import com.taxworkbench.domain.shared.ClientType;

public record ClientListQuery(
        String name,
        ClientStatus status,
        ClientType type,
        ClientTier tier,
        int pageSize,
        String cursor
) {
}
