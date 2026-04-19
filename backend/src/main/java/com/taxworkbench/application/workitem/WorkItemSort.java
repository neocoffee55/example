package com.taxworkbench.application.workitem;

import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record WorkItemSort(
        Sort sort,
        boolean cursorCompatible
) {
    private static final String DEFAULT_SORT_TOKEN = "dueDate:asc,clientName:asc,id:asc";
    private static final Set<String> ALLOWED_FIELDS = Set.of("dueDate", "clientName", "status", "assignee", "updatedAt", "id");

    public static WorkItemSort parse(String rawSort) {
        if (rawSort == null || rawSort.isBlank()) {
            return defaultSort();
        }

        List<Sort.Order> orders = new ArrayList<>();
        List<String> normalized = new ArrayList<>();
        for (String token : rawSort.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split(":", 2);
            String field = parts[0].trim();
            String direction = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
            if (!ALLOWED_FIELDS.contains(field)) {
                throw new IllegalArgumentException("Unsupported sort field: " + field);
            }
            if (!direction.equals("asc") && !direction.equals("desc")) {
                throw new IllegalArgumentException("Unsupported sort direction: " + direction);
            }
            normalized.add(field + ":" + direction);
            orders.add(new Sort.Order(direction.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, toProperty(field)));
        }

        if (orders.isEmpty()) {
            return defaultSort();
        }

        if (orders.stream().noneMatch(order -> order.getProperty().equals("id"))) {
            orders.add(Sort.Order.asc("id"));
            normalized.add("id:asc");
        }

        boolean cursorCompatible = String.join(",", normalized).equals(DEFAULT_SORT_TOKEN);
        return new WorkItemSort(Sort.by(orders), cursorCompatible);
    }

    public static WorkItemSort defaultSort() {
        return new WorkItemSort(
                Sort.by(Sort.Order.asc("dueDate"), Sort.Order.asc("client.name"), Sort.Order.asc("id")),
                true
        );
    }

    private static String toProperty(String field) {
        return switch (field) {
            case "clientName" -> "client.name";
            default -> field;
        };
    }
}
