package com.taxworkbench;

import static org.assertj.core.api.Assertions.assertThat;

import com.taxworkbench.application.WorkbenchDataService;
import com.taxworkbench.application.WorkbenchDataService.WorkItemAuditView;
import com.taxworkbench.application.WorkbenchDataService.WorkItemPayload;
import com.taxworkbench.application.WorkbenchDataService.WorkItemView;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "tax-workbench.export.batch-size=1"
})
class WorkItemQueryIntegrationTests {

    @Autowired
    private WorkbenchDataService workbenchDataService;

    @Test
    void findWorkItemsAppliesFiltersInDatabaseOrder() {
        List<WorkItemView> result = workbenchDataService.findWorkItems("clinic", "", "", "");

        assertThat(result)
                .extracting(WorkItemView::id)
                .containsExactly("WI-10032");
    }

    @Test
    void streamWorkItemsAsCsvWritesFilteredRowsInSortedOrder() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        workbenchDataService.streamWorkItemsAsCsv("", "", "", "", outputStream);

        String csv = outputStream.toString(StandardCharsets.UTF_8);

        assertThat(csv).contains("업체명,사업자번호,업무유형,상태,담당자,마감일,최근수정");
        assertThat(csv).contains("\"Han River Holdings\",\"123-45-67890\",\"FILING\",\"IN_PROGRESS\",\"insu\",\"2026-03-20\"");
        assertThat(csv).contains("\"Mirae Clinic\",\"220-11-90876\",\"REVIEW\",\"HOLD\",\"jane\",\"2026-03-18\"");
        assertThat(csv.indexOf("Han River Holdings")).isLessThan(csv.indexOf("Mirae Clinic"));
    }

    @Test
    void findWorkItemAuditLogsByBizNoReturnsMergedLogsInChangedAtDescOrder() {
        workbenchDataService.createWorkItem(
                new WorkItemPayload(
                        "WI-AUDIT-BIZNO-1001",
                        0,
                        "Han River Branch",
                        "123-45-67890",
                        "BOOKKEEPING",
                        "TODO",
                        "insu",
                        "2026-03-25",
                        "insu"
                )
        );

        workbenchDataService.updateWorkItem(
                "WI-10031",
                new WorkItemPayload(
                        "WI-10031",
                        0,
                        "Han River Holdings",
                        "123-45-67890",
                        "REVIEW",
                        "IN_PROGRESS",
                        "insu",
                        "2026-03-21",
                        "insu"
                )
        );

        workbenchDataService.updateWorkItem(
                "WI-AUDIT-BIZNO-1001",
                new WorkItemPayload(
                        "WI-AUDIT-BIZNO-1001",
                        0,
                        "Han River Branch",
                        "123-45-67890",
                        "ETC",
                        "DONE",
                        "insu",
                        "2026-03-26",
                        "insu"
                )
        );

        List<WorkItemAuditView> logs = workbenchDataService.findWorkItemAuditLogsByBizNo("123-45-67890");

        assertThat(logs).isNotEmpty();
        assertThat(logs)
                .extracting(WorkItemAuditView::workItemId)
                .contains("WI-10031", "WI-AUDIT-BIZNO-1001");
        assertThat(logs)
                .extracting(WorkItemAuditView::changedAt)
                .isSortedAccordingTo((left, right) -> right.compareTo(left));
    }
}
