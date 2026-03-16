# Tax Workbench

Step 1 skeleton for a production-oriented tax operations workbench.

## Workspace

- `backend`: Spring Boot 4.0.0, Java 21 target, DDD-oriented package boundaries
- `frontend`: React 19, TypeScript, Tailwind CSS, TanStack Query-ready UI shell

## Backend package direction

- `com.taxworkbench.domain`: entities, value objects, policies, rules
- `com.taxworkbench.application`: use cases, commands, queries, transaction orchestration
- `com.taxworkbench.infrastructure`: JPA mappings, repository adapters, batch/export implementations
- `com.taxworkbench.interfaces`: REST DTOs, controllers, exception mapping

## Frontend package direction

- `src/workbench`: workbench shell, later grid/filter/drawer modules
- `src/api`: query and mutation clients
- `src/features`: screen-level capabilities such as listing, edit conflict, audit
- `src/shared`: reusable UI and utility code

## Run

- Backend: [docs/backend-run.md](/Users/insu_han/IdeaProjects/example/docs/backend-run.md)
- Frontend: [docs/frontend-run.md](/Users/insu_han/IdeaProjects/example/docs/frontend-run.md)

## Notes

- The local machine currently exposes Maven on Java 23, while the backend build targets Java 21.
- Step 2 should introduce the actual domain model and persistence mapping before enabling schema generation.
