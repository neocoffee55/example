package com.taxworkbench.application.client;

import com.taxworkbench.application.shared.CursorPage;

public interface ClientQueryUseCase {

    CursorPage<ClientView> listClients(ClientListQuery query);
}
