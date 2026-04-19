package com.taxworkbench.application.workitem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkItemSortTests {

    @Test
    void defaultSortIsCursorCompatible() {
        WorkItemSort sort = WorkItemSort.parse(null);

        assertTrue(sort.cursorCompatible());
        assertEquals(3, sort.sort().stream().toList().size());
    }

    @Test
    void customSortGetsStableIdTieBreaker() {
        WorkItemSort sort = WorkItemSort.parse("status:desc");

        assertFalse(sort.cursorCompatible());
        assertEquals("status", sort.sort().stream().toList().get(0).getProperty());
        assertEquals("id", sort.sort().stream().toList().get(1).getProperty());
    }

    @Test
    void unsupportedSortFieldIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> WorkItemSort.parse("memo:asc"));
    }
}
