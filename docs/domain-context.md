# Domain Context

## Core Aggregates

### Client

- Owns identity and business classification
- Fields: `id`, `name`, `bizNo`, `type`, `status`, `tier`
- Acts as a policy source for related work items

### WorkItem

- Represents a unit of operational tax work
- Fields from the assignment plus operational metadata
- Always belongs to one client
- Mutations are policy-checked against current client attributes

### AuditLog

- Append-only record of field-level changes
- Stores actor, timestamp, field name, before value, after value, and source action
- Queried separately from the main listing model

## Domain Invariants

- A work item must always reference an existing client
- Inactive clients cannot receive new work items
- Client type and tier may constrain allowed work item creation or updates
- Changes that affect integrity must be version-checked
- Audit records are immutable once written

## Candidate Business Rules

These should be encoded as policies, not scattered through controllers or UI.

### Client Status Rules

- `Client.status = INACTIVE` blocks new `WorkItem` creation
- Existing work items linked to inactive clients may become read-only or `HOLD`

### Tier Rules

- `VIP` clients require stronger assignment guarantees
- VIP changes should be visually emphasized in the UI
- VIP work may require dedicated assignee validation

### Type Rules

- `CORPORATE` and `INDIVIDUAL` clients may have different default work item templates
- Validation and due date logic may differ by client type

## Domain Events To Model Early

- `WorkItemCreated`
- `WorkItemUpdated`
- `WorkItemConflictDetected`
- `ClientStatusChanged`
- `ClientTierChanged`
- `AuditLogRecorded`
- `BulkImportAccepted`
- `ExportRequested`

## Open Domain Decisions

- Whether a client status change should cascade updates into related work items synchronously or asynchronously
- Whether work item type defaults are generated on creation or selected explicitly by the user
- Whether VIP assignment is hard-blocking or warning-only
