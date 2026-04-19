# Implementation Backlog

## Phase 0: Baseline

- Recreate repo skeleton for `backend`, `frontend`, and `docs`
- Add root `README.md` with project intent and setup placeholders
- Define coding conventions and package boundaries

## Phase 1: Domain First

- Model `Client`, `WorkItem`, and `AuditLog`
- Add enums and policy objects for tier/type/status rules
- Define domain events and versioning strategy
- Write domain tests for client-to-work-item policy interactions

## Phase 2: Query And Mutation Contracts

- Design API contracts for listing filters, sorting, paging, inline updates, create, bulk insert, export, and audit history
- Define conflict response schema for optimistic locking failures
- Decide whether export and import are synchronous or job-backed in the first version

## Phase 3: Backend Slice

- Implement persistence schema and repositories
- Implement command/query services
- Implement inline edit with optimistic locking
- Implement audit recording on mutable fields
- Implement filtered listing with indexed query path

## Phase 4: Heavy Data Paths

- Implement bulk insert batching and failure reporting
- Implement CSV export streaming over the filtered query
- Add integration tests for large batch and export paths

## Phase 5: Frontend Workbench

- Build workbench shell and filter bar
- Build virtualized grid with keyboard navigation
- Add inline editing for status and due date
- Add create-work-item flow with client assignment
- Add conflict resolution panel/modal
- Add audit history side panel

## Phase 6: Production Readiness

- Add API error taxonomy and user-facing error handling
- Add observability hooks for import, export, and conflict events
- Document cloud expansion strategy in README
- Add performance notes and known trade-offs

## Acceptance Checklist

- Listing, filtering, sorting, and paging work
- Inline edit works and conflicts are surfaced
- New work item creation respects client policies
- Bulk insert handles thousands of records
- Export downloads current filtered results as CSV
- Audit UI shows field-level history
- README documents architecture and AI collaboration
