package com.taxworkbench.application.client;

public interface ClientCommandUseCase {

    ClientView createClient(CreateClientCommand command);

    ClientView updateClient(UpdateClientCommand command);
}
