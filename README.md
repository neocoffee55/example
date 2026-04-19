# Tax Workbench

Tax Workbench is a keyboard-first operations console for high-volume tax work management. This repository contains a Spring Boot backend slice and a React workbench frontend built around the assignment constraints: inline edits, optimistic locking, audit history, bulk import, filtered export, and sustainable context-driven architecture.

## Repository Layout

- `backend/`: Java 21 + Spring Boot API with H2, JPA, optimistic locking, audit persistence, and seeded demo data
- `frontend/`: React + TypeScript + Tailwind workbench UI for filtering, inline editing, audit review, bulk import, and export
- `docs/`: product context, domain rules, architecture notes, API contract, and implementation backlog
- `00_개발과제.md`: original assignment brief

## What Is Implemented

### Backend

- `GET /api/work-items`
- `POST /api/work-items`
- `PATCH /api/work-items/{id}`
- `GET /api/work-items/{id}/audit-logs`
- `POST /api/work-items/bulk-import`
- `GET /api/work-items/export`
- `GET /api/clients`
- `POST /api/clients`
- `PATCH /api/clients/{id}`

The backend persists `Client`, `WorkItem`, and `AuditLog` through JPA. `PATCH` mutations are version-aware and return structured conflict payloads. Work item and client lists use cursor envelopes. Bulk import supports partial failures. Export writes CSV page-by-page instead of materializing the full result set first.

### Frontend

- filter bar for client name, assignee, and status chips
- inline status and due date editing
- conflict banner with field-level base/attempted/current values
- audit side panel for the selected work item
- export trigger for the current filtered slice
- bulk import form using the backend JSON contract
- keyboard row navigation with arrow keys

## How To Run

### Backend

See [docs/backend-run.md](./docs/backend-run.md).

### Frontend

See [docs/frontend-run.md](./docs/frontend-run.md).

## Architectural Rationale

The code is intentionally split into `domain`, `application`, `infrastructure`, and `interfaces` boundaries so the business rules do not collapse into controllers or persistence models. The current slice keeps the domain lean but still forces all write paths through application services where policy checks, optimistic locking, and audit writes live together.

For scalability, read-heavy concerns and write integrity concerns are kept separate at the contract level. Audit history has its own read path. Export does not reuse the paged UI response directly. Conflict handling is treated as a product concern rather than a hidden retry concern, so the API carries structured merge information back to the workbench.

## Trade-offs

- Cursor pagination is fully supported for the default list order, but arbitrary sort plus cursor is intentionally deferred
- Export is page-streamed rather than low-level database streaming, which is better than full in-memory aggregation but not the final end-state
- The current backend uses H2 and seeded demo data for local verification, not a production-grade database topology
- Frontend state is local and pragmatic; it does not yet use a query/state library

## Cloud And PaaS Strategy

If this moved to production scale, the next steps would be:

- move the relational store to managed Postgres or Azure Database for PostgreSQL
- shift bulk import and export orchestration behind async jobs
- push import/export jobs through a queue such as Service Bus
- run API and worker processes independently in Container Apps or another autoscaling container platform
- keep audit data append-only and partitioned by work item or time window
- add observability around conflict rate, import failure rate, export latency, and query hot spots

## AI Collaboration Reflection

AI was used as a constrained implementation partner rather than a generator of generic CRUD. The work started by pinning down product context, domain rules, architecture boundaries, and API scenarios in `docs/` before code generation. That context pack was then used to keep the implementation aligned with the assignment's non-trivial concerns: conflict handling, auditing, streaming, and policy-driven behavior.

The main limitation of AI-generated code here is that it tends to overproduce flat layered structures and under-specify conflict semantics. That was handled by explicitly forcing the design around command/query contracts, cursor rules, and field-level conflict payloads before implementation.

## Verification Status

- `frontend`: `npm install` completed and `npm run build` passed on April 20, 2026
- `backend`: code was created and aligned to the documented API, but build verification could not be executed in this environment because Java/Maven were not installed

## Primary Reference Docs

- [docs/product-context.md](./docs/product-context.md)
- [docs/domain-context.md](./docs/domain-context.md)
- [docs/domain-rules-matrix.md](./docs/domain-rules-matrix.md)
- [docs/architecture-context.md](./docs/architecture-context.md)
- [docs/api-spec.md](./docs/api-spec.md)
- [docs/api-scenarios.md](./docs/api-scenarios.md)
- [docs/state-transitions.md](./docs/state-transitions.md)
