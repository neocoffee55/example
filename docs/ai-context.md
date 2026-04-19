# AI Context

## Purpose

This document is the stable context pack for AI-assisted development on the Tax Workbench project. Feed it with the relevant task-specific document before asking the AI to implement or review anything.

## Global Instructions

- Do not generate a generic layered CRUD app
- Keep domain rules in domain or application policy code, not in controllers
- Model `Client` and `WorkItem` interaction explicitly
- Assume 100,000+ rows and concurrent editing are core requirements, not future concerns
- Design bulk insert and export as streaming or batched flows
- Treat audit history as append-only and isolated from main listing performance
- Expose concurrency conflicts to the user with structured resolution data
- Favor explicit trade-offs over magical abstractions

## Mandatory Constraints

- Inline edits must be version-aware
- Current filtered results must be exportable
- Audit UI must show field-level before and after values
- Client tier and type must influence work item behavior
- README must explain rationale, trade-offs, PaaS strategy, and AI collaboration reflection

## Prompt Template For Feature Work

```text
Project: Tax Workbench
Task: <feature or refactor>

Use these constraints:
- Domain rules must not live in controllers or React components
- Assume concurrent editing and 100k+ rows
- Export must stream
- Audit history must remain append-only and cheap for listing path
- Favor clear contracts over framework convenience

Relevant context files:
- docs/product-context.md
- docs/domain-context.md
- docs/architecture-context.md

Deliver:
1. Implementation
2. Key architectural decisions
3. Risks or trade-offs
4. Tests added
```

## Prompt Template For Code Review

```text
Review this change for:
- broken domain invariants
- hidden concurrency issues
- listing/query performance regressions
- audit logging gaps
- framework coupling inside business logic
- missing tests for policy behavior
```

## Operating Rule

When AI suggests a shortcut, compare it against the product constraints first. If it weakens concurrency, auditability, or scalability, reject it even if it reduces code volume.
