package com.taxworkbench;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.taxworkbench.application.WorkbenchDataService;
import com.taxworkbench.application.WorkbenchDataService.BulkInsertRequest;
import com.taxworkbench.application.WorkbenchDataService.BulkInsertFailure;
import com.taxworkbench.application.WorkbenchDataService.BulkInsertResult;
import com.taxworkbench.application.WorkbenchDataService.WorkItemPayload;
import com.taxworkbench.infrastructure.persistence.WorkItemJpaRepository;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "tax-workbench.bulk-insert.max-request-size=10000",
        "tax-workbench.bulk-insert.chunk-size=500"
})
class WorkItemBulkInsertIntegrationTests {

    @Autowired
    private WorkbenchDataService workbenchDataService;

    @Autowired
    private WorkItemJpaRepository workItemRepository;

    @Test
    void bulkInsertStoresMultipleRowsWhenAllItemsAreValid() throws Exception {
        String firstId = "WI-BULK-SUCCESS-1001";
        String secondId = "WI-BULK-SUCCESS-1002";

        BulkInsertResult response = workbenchDataService.bulkInsertWorkItems(
                new BulkInsertRequest(List.of(
                        new WorkItemPayload(firstId, 0, "Bulk Client Alpha", "111-11-11111", "FILING", "TODO", "insu", "2026-03-25", "insu"),
                        new WorkItemPayload(secondId, 0, "Bulk Client Beta", "111-11-11111", "REVIEW", "TODO", "insu", "2026-03-26", "insu")
                ))
        );

        assertThat(response).isNotNull();
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.successCount()).isEqualTo(2);
        assertThat(response.failureCount()).isEqualTo(0);

        assertThat(workItemRepository.findById(firstId)).isPresent();
        assertThat(workItemRepository.findById(secondId)).isPresent();
    }

    @Test
    void bulkInsertReturnsPartialFailureWhenSomeItemsAreInvalid() throws Exception {
        String validId = "WI-BULK-PARTIAL-1001";

        BulkInsertResult response = workbenchDataService.bulkInsertWorkItems(
                new BulkInsertRequest(List.of(
                        new WorkItemPayload(validId, 0, "Bulk Client Gamma", "333-33-33333", "FILING", "TODO", "insu", "2026-03-27", "insu"),
                        new WorkItemPayload("WI-10031", 0, "Duplicated Existing Row", "444-44-44444", "REVIEW", "TODO", "insu", "2026-03-28", "insu")
                ))
        );

        assertThat(response).isNotNull();
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.failureCount()).isEqualTo(1);
        assertThat(response.failures())
                .extracting(BulkInsertFailure::workItemId, BulkInsertFailure::reason)
                .containsExactly(tuple("WI-10031", "이미 존재하는 WorkItem id입니다."));

        assertThat(workItemRepository.findById(validId)).isPresent();
        assertThat(workItemRepository.findAllById(List.of("WI-10031"))).hasSize(1);
    }

    @Test
    void bulkInsertStoresTenThousandRowsBasedOnTwoPopupClients() {
        long beforeCount = workItemRepository.count();

        List<WorkItemPayload> items = IntStream.range(0, 10_000)
                .mapToObj(index -> {
                    boolean useFirstClient = index % 2 == 0;
                    return new WorkItemPayload(
                            "WI-BULK-10K-%05d".formatted(index),
                            0,
                            useFirstClient ? "Han River Holdings" : "Mirae Clinic",
                            useFirstClient ? "123-45-67890" : "220-11-90876",
                            useFirstClient ? "FILING" : "REVIEW",
                            "TODO",
                            "insu",
                            "2026-03-31",
                            "insu"
                    );
                })
                .toList();

        BulkInsertResult result = workbenchDataService.bulkInsertWorkItems(new BulkInsertRequest(items));

        assertThat(result.totalCount()).isEqualTo(10_000);
        assertThat(result.successCount()).isEqualTo(10_000);
        assertThat(result.failureCount()).isEqualTo(0);
        assertThat(workItemRepository.count()).isEqualTo(beforeCount + 10_000);
        assertThat(workItemRepository.findById("WI-BULK-10K-00000")).isPresent();
        assertThat(workItemRepository.findById("WI-BULK-10K-09999")).isPresent();
    }
}
