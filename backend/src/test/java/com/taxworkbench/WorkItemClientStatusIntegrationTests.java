package com.taxworkbench;

import static org.assertj.core.api.Assertions.assertThat;

import com.taxworkbench.application.WorkbenchDataService;
import com.taxworkbench.application.WorkbenchDataService.WorkItemPayload;
import com.taxworkbench.application.WorkbenchDataService.WorkItemView;
import com.taxworkbench.infrastructure.persistence.ClientEntity;
import com.taxworkbench.infrastructure.persistence.ClientJpaRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WorkItemClientStatusIntegrationTests {

    @Autowired
    private WorkbenchDataService workbenchDataService;

    @Autowired
    private ClientJpaRepository clientRepository;

    @Test
    void updateWorkItemActivatesClientWithSameBizNo() {
        ClientEntity client = clientRepository.findById("CL-1001").orElseThrow();
        WorkItemView workItem = workbenchDataService.findWorkItems("Han River Holdings", "", "", "").stream()
                .filter(item -> "WI-10031".equals(item.id()))
                .findFirst()
                .orElseThrow();

        clientRepository.save(new ClientEntity(
                client.getId(),
                client.getName(),
                client.getBizNo(),
                client.getType(),
                "INACTIVE",
                client.getTier(),
                Instant.now()
        ));

        workbenchDataService.updateWorkItem(
                "WI-10031",
                new WorkItemPayload(
                        "WI-10031",
                        workItem.revision(),
                        "Han River Holdings",
                        "123-45-67890",
                        "FILING",
                        "DONE",
                        "insu",
                        "2026-03-20",
                        "insu"
                )
        );

        assertThat(clientRepository.findById("CL-1001").orElseThrow().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void deleteWorkItemDeactivatesClientWhenNoWorkItemsRemain() {
        WorkItemView workItem = workbenchDataService.findWorkItems("Mirae Clinic", "", "", "").stream()
                .filter(item -> "WI-10032".equals(item.id()))
                .findFirst()
                .orElseThrow();

        workbenchDataService.deleteWorkItem("WI-10032", workItem.revision(), "insu");

        assertThat(clientRepository.findById("CL-1002").orElseThrow().getStatus()).isEqualTo("INACTIVE");
    }
}
