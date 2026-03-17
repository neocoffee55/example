package com.taxworkbench;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.taxworkbench.application.WorkbenchDataService;
import com.taxworkbench.application.WorkbenchDataService.ClientPayload;
import com.taxworkbench.application.WorkbenchDataService.ClientSaveRequest;
import com.taxworkbench.application.WorkbenchDataService.ClientView;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ClientSaveIntegrationTests {

    @Autowired
    private WorkbenchDataService workbenchDataService;

    @Test
    void saveClientsRejectsDuplicatedBizNo() {
        assertThatThrownBy(() -> workbenchDataService.saveClients(new ClientSaveRequest(
                List.of(
                        new ClientPayload("CL-DUP-1001", "Alpha Tax", "123-45-67890", "CORPORATE", "ACTIVE", "BASIC"),
                        new ClientPayload("CL-DUP-1002", "Beta Tax", "123-45-67890", "CORPORATE", "ACTIVE", "VIP")
                ),
                List.of()
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("동일한 사업자번호가 존재합니다.");
    }

    @Test
    void saveClientsRejectsBizNoThatAlreadyExistsInDatabaseEvenWithoutHyphen() {
        ClientView existingClient = workbenchDataService.findClients("").stream()
                .findFirst()
                .orElseThrow();

        assertThatThrownBy(() -> workbenchDataService.saveClients(new ClientSaveRequest(
                List.of(
                        new ClientPayload(
                                "CL-NEW-1001",
                                "Alpha Tax",
                                existingClient.bizNo().replace("-", ""),
                                "CORPORATE",
                                "ACTIVE",
                                "BASIC"
                        )
                ),
                List.of()
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("동일한 사업자번호가 존재합니다.");
    }

    @Test
    void saveClientsKeepsExistingRowsWhenSavingSubsetWithNewClient() {
        List<ClientView> before = workbenchDataService.findClients("");

        List<ClientView> saved = workbenchDataService.saveClients(new ClientSaveRequest(
                List.of(
                        new ClientPayload("CL-NEW-2001", "New Client", "555-55-55555", "CORPORATE", "ACTIVE", "BASIC")
                ),
                List.of()
        ));

        assertThat(saved).hasSize(before.size() + 1);
        assertThat(saved).extracting(ClientView::id)
                .contains("CL-1001", "CL-1002", "CL-NEW-2001");
    }
}
