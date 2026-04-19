# Architecture Context

## Guiding Principles

- Keep business rules independent from framework details
- Separate write integrity from read performance
- Treat concurrency conflicts as a first-class product concern
- Prefer append-only and streaming patterns for heavy data paths
- Optimize for maintainability under changing rules

## Recommended Backend Structure

```text
backend/
  src/main/java/com/taxworkbench/
    domain/
      client/
      workitem/
      audit/
      shared/
    application/
      command/
      query/
      policy/
    infrastructure/
      persistence/
      audit/
      streaming/
      config/
    interfaces/
      http/
      batch/
```

## Recommended Frontend Structure

```text
frontend/
  src/
    app/
    pages/workbench/
    features/work-items/
    features/clients/
    features/audit-history/
    features/import-export/
    components/grid/
    components/conflict-resolution/
    lib/api/
    lib/state/
    styles/
```

## Tactical Decisions

### Listing

- Use a dedicated query model for filtering, sorting, and paging
- Design for keyset/cursor pagination even if the first cut starts with indexed offset pagination
- Keep audit joins out of the listing query path

### Inline Editing

- Use optimistic locking with explicit `version` comparison
- Return conflict payloads that include base value, current value, and attempted value
- Let the UI drive conflict resolution rather than silently retrying

### Auditing

- Persist audit records as append-only entries
- Write audit records through a narrow application service or event handler
- Store audit history separately from the list read model

### Bulk Insert

- Accept import requests in batches
- Validate row-level failures without aborting the whole batch unless integrity requires it
- Keep import processing idempotent where practical

### Export

- Reuse the same filter contract as listing
- Stream rows directly to CSV output
- Never materialize the whole export in memory

## Initial Technical Position

- Backend: Java 21 + Spring Boot with H2 for local execution
- Frontend: React 19 + TypeScript + Tailwind CSS
- UI grid: keyboard-first, virtualized, conflict-aware
- Testing: domain policy tests first, API integration tests second, UI interaction tests for the workbench flows
