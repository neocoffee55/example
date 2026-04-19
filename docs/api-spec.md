# API Specification

This document is the implementation contract for the Tax Workbench HTTP API.

## Scope

This API supports:

- large-scale work item listing
- inline work item updates with optimistic locking
- work item creation under client policy constraints
- field-level audit history lookup
- bulk import
- filtered CSV export
- client lookup and client status updates

## Global Conventions

### Base Path

- Base path: `/api`

### Content Types

- Request: `application/json`
- Standard response: `application/json`
- Export response: `text/csv`

### Time And Date Format

- Timestamp fields use ISO-8601 UTC, for example `2026-04-16T11:25:00Z`
- Date-only fields use `YYYY-MM-DD`, for example `2026-03-20`

### IDs

- Resource IDs are numeric in the first version

### Versioning And Concurrency

- Every mutable `WorkItem` and `Client` carries a numeric `version`
- Mutation requests must send the last observed `version`
- If the stored version differs, the server returns `409 Conflict`

### Pagination

- Listing and audit history use cursor pagination
- Request parameter: `cursor`
- Request parameter: `pageSize`
- Default `pageSize`: `50`
- Maximum `pageSize`: `200`
- Cursor is opaque to clients

### Sorting

- Multi-column sorting is supported where documented
- Sort format: `field:direction,field:direction`
- Direction values: `asc`, `desc`
- Current backend slice supports cursor pagination only with the default work-item sort

### Error Envelope

Unless otherwise noted, error responses use this shape:

```json
{
  "code": "INVALID_REQUEST",
  "message": "Human-readable summary",
  "details": []
}
```

## Enums

### WorkItemType

- `FILING`
- `BOOKKEEPING`
- `REVIEW`
- `ETC`

### WorkItemStatus

- `TODO`
- `IN_PROGRESS`
- `DONE`
- `HOLD`

### ClientType

- `INDIVIDUAL`
- `CORPORATE`

### ClientStatus

- `ACTIVE`
- `INACTIVE`

### ClientTier

- `BASIC`
- `PREMIUM`
- `VIP`

### AuditSource

- `INLINE_EDIT`
- `CREATE`
- `BULK_IMPORT`
- `SYSTEM_RULE`
- `CLIENT_POLICY_EFFECT`

## Shared Models

### WorkItemView

```json
{
  "id": 101,
  "clientId": 20,
  "clientName": "Hanbit Tax",
  "bizNo": "123-45-67890",
  "type": "FILING",
  "status": "TODO",
  "assignee": "kim",
  "dueDate": "2026-03-20",
  "tags": ["march", "priority"],
  "memo": "priority filing",
  "updatedAt": "2026-04-16T11:22:00Z",
  "version": 4,
  "clientType": "CORPORATE",
  "clientTier": "VIP",
  "clientStatus": "ACTIVE"
}
```

### ClientView

```json
{
  "id": 20,
  "name": "Hanbit Tax",
  "bizNo": "123-45-67890",
  "type": "CORPORATE",
  "status": "ACTIVE",
  "tier": "VIP",
  "version": 2,
  "updatedAt": "2026-04-16T11:10:00Z"
}
```

### PageEnvelope

```json
{
  "items": [],
  "page": {
    "nextCursor": "opaque-token",
    "pageSize": 50,
    "hasNext": true
  }
}
```

## Filter Contract

Listing and export must use the same filter semantics.

### WorkItem Filters

| Name | Type | Required | Notes |
| --- | --- | --- | --- |
| `clientName` | string | No | Partial match, case-insensitive |
| `statuses` | comma-separated enum list | No | Example: `TODO,HOLD` |
| `assignees` | comma-separated string list | No | Exact match per token |
| `dueDateFrom` | date | No | Inclusive |
| `dueDateTo` | date | No | Inclusive |
| `clientType` | enum | No | `INDIVIDUAL` or `CORPORATE` |
| `clientTier` | enum | No | `BASIC`, `PREMIUM`, `VIP` |
| `sort` | string | No | Multi-sort string |
| `pageSize` | integer | No | Min `1`, max `200` |
| `cursor` | string | No | Opaque token |

### Allowed Sort Fields

- `dueDate`
- `clientName`
- `status`
- `assignee`
- `updatedAt`

### Default Sort

- `dueDate:asc,clientName:asc,id:asc`

## Endpoints

## `GET /api/work-items`

Lists work items for the workbench grid.

### Query Parameters

Uses the shared WorkItem filter contract.

### Response `200 OK`

```json
{
  "items": [
    {
      "id": 101,
      "clientId": 20,
      "clientName": "Hanbit Tax",
      "bizNo": "123-45-67890",
      "type": "FILING",
      "status": "TODO",
      "assignee": "kim",
      "dueDate": "2026-03-20",
      "tags": ["march"],
      "memo": "priority filing",
      "updatedAt": "2026-04-16T11:22:00Z",
      "version": 4,
      "clientType": "CORPORATE",
      "clientTier": "VIP",
      "clientStatus": "ACTIVE"
    }
  ],
  "page": {
    "nextCursor": "opaque-token-2",
    "pageSize": 50,
    "hasNext": true
  }
}
```

### Notes

- Audit history is not embedded in this response
- This endpoint must remain performant at large dataset sizes
- If `cursor` is provided, `sort` must be omitted or match the default sort in the current backend slice

## `POST /api/work-items`

Creates a single work item.

### Request Body

```json
{
  "clientId": 20,
  "type": "FILING",
  "status": "TODO",
  "assignee": "kim",
  "dueDate": "2026-03-20",
  "tags": ["march"],
  "memo": "priority filing"
}
```

### Validation Rules

- `clientId` is required
- `type` is required
- `status` is required
- `dueDate` is required
- `tags` defaults to empty array if omitted
- `memo` is optional
- Client policy must be checked before creation

### Response `201 Created`

Returns `WorkItemView`.

### Policy Errors

- Inactive clients cannot receive new work items
- VIP client work items require `assignee`
- Client type may restrict allowed work item types

## `PATCH /api/work-items/{id}`

Updates mutable work item fields through operation-based inline edits.

### Request Body

```json
{
  "version": 4,
  "operations": [
    {
      "field": "status",
      "baseValue": "TODO",
      "value": "IN_PROGRESS"
    },
    {
      "field": "dueDate",
      "baseValue": "2026-03-20",
      "value": "2026-03-25"
    }
  ]
}
```

### Field Rules

| Field | Value Type | Notes |
| --- | --- | --- |
| `status` | enum | Uses `WorkItemStatus` |
| `dueDate` | date | `YYYY-MM-DD` |
| `assignee` | string or null | Null may be rejected for VIP clients |
| `memo` | string or null | Audited |
| `tags` | array of strings | Full replacement semantics in v1 |

### Constraints

- `version` is required
- `operations` must contain at least one item
- `operations[].baseValue` is optional but recommended for richer conflict comparison UX
- Unknown fields are rejected
- All operations are applied atomically
- All successful updates write audit entries for changed audited fields

### Response `200 OK`

Returns the updated `WorkItemView`.

### Conflict Response `409 Conflict`

```json
{
  "code": "WORK_ITEM_CONFLICT",
  "message": "The work item was modified by another user.",
  "resourceId": 101,
  "currentVersion": 6,
  "updatedBy": "alice",
  "updatedAt": "2026-04-16T11:27:00Z",
  "fieldConflicts": [
    {
      "field": "dueDate",
      "baseValue": "2026-03-20",
      "attemptedValue": "2026-03-25",
      "currentValue": "2026-03-22"
    }
  ]
}
```

### Policy Failure Response `422 Unprocessable Entity`

```json
{
  "code": "CLIENT_POLICY_VIOLATION",
  "message": "VIP client work items require an assignee.",
  "details": [
    {
      "field": "assignee",
      "reason": "required_for_vip_client"
    }
  ]
}
```

## `GET /api/work-items/{id}/audit-logs`

Returns audit history for a single work item.

### Query Parameters

| Name | Type | Required | Notes |
| --- | --- | --- | --- |
| `pageSize` | integer | No | Default `50`, max `200` |
| `cursor` | string | No | Opaque token |

### Response `200 OK`

```json
{
  "items": [
    {
      "auditLogId": 8001,
      "workItemId": 101,
      "fieldName": "status",
      "beforeValue": "TODO",
      "afterValue": "IN_PROGRESS",
      "actorId": "user-1",
      "actorName": "kim",
      "source": "INLINE_EDIT",
      "changedAt": "2026-04-16T11:25:00Z"
    }
  ],
  "page": {
    "nextCursor": "opaque-token-2",
    "pageSize": 50,
    "hasNext": true
  }
}
```

### Notes

- Ordered newest first
- Uses a dedicated query path, not the listing path

## `POST /api/work-items/bulk-import`

Imports many work items in one request.

### Request Body

```json
{
  "requestId": "import-2026-04-16-001",
  "items": [
    {
      "clientId": 20,
      "type": "FILING",
      "status": "TODO",
      "assignee": "kim",
      "dueDate": "2026-03-20",
      "tags": ["march"],
      "memo": "priority filing"
    }
  ]
}
```

### Rules

- `requestId` is required in v1 for retry traceability
- `items` must contain at least one row
- Row validation is independent per item
- Partial success is allowed
- Unknown clients, inactive clients, and VIP-without-assignee rows fail per row

### Response `200 OK`

```json
{
  "requestId": "import-2026-04-16-001",
  "summary": {
    "received": 2,
    "created": 1,
    "failed": 1
  },
  "failures": [
    {
      "row": 2,
      "code": "CLIENT_NOT_FOUND",
      "message": "Client 99 does not exist."
    }
  ]
}
```

### Notes

- Future versions may support async job execution for larger imports
- Import-generated field changes must still write audit records

## `GET /api/work-items/export`

Streams the current filtered result set as CSV.

### Query Parameters

Uses the same filter contract as `GET /api/work-items`, except `pageSize` and `cursor` are ignored.

### Response `200 OK`

Headers:

```text
Content-Type: text/csv
Content-Disposition: attachment; filename="work-items-2026-04-16.csv"
Transfer-Encoding: chunked
```

### CSV Columns

Columns in v1:

- `id`
- `clientName`
- `bizNo`
- `type`
- `status`
- `assignee`
- `dueDate`
- `tags`
- `memo`
- `updatedAt`
- `clientType`
- `clientTier`
- `clientStatus`

### Rules

- Must stream directly without loading the full result into memory
- Must reflect the current filter contract used by listing
- Sort order must match the supplied sort query

## `GET /api/clients`

Lists clients for lookup and assignment flows.

### Query Parameters

| Name | Type | Required | Notes |
| --- | --- | --- | --- |
| `name` | string | No | Partial match |
| `status` | enum | No | `ACTIVE` or `INACTIVE` |
| `type` | enum | No | `INDIVIDUAL` or `CORPORATE` |
| `tier` | enum | No | `BASIC`, `PREMIUM`, `VIP` |
| `pageSize` | integer | No | Default `50`, max `200` |
| `cursor` | string | No | Opaque token |

### Response `200 OK`

```json
{
  "items": [
    {
      "id": 20,
      "name": "Hanbit Tax",
      "bizNo": "123-45-67890",
      "type": "CORPORATE",
      "status": "ACTIVE",
      "tier": "VIP",
      "version": 2,
      "updatedAt": "2026-04-16T11:10:00Z"
    }
  ],
  "page": {
    "nextCursor": null,
    "pageSize": 50,
    "hasNext": false
  }
}
```

## `POST /api/clients`

Creates a client.

### Request Body

```json
{
  "name": "Hanbit Tax",
  "bizNo": "123-45-67890",
  "type": "CORPORATE",
  "status": "ACTIVE",
  "tier": "VIP"
}
```

### Response `201 Created`

Returns `ClientView`.

## `PATCH /api/clients/{id}`

Updates client mutable fields in v1, primarily `status` and `tier`.

### Request Body

```json
{
  "version": 2,
  "status": "INACTIVE",
  "tier": "VIP"
}
```

### Rules

- `version` is required
- Unknown mutable fields are rejected
- Status changes affect future work item creation immediately
- Tier changes affect future work item validation immediately

### Response `200 OK`

Returns `ClientView`.

### Conflict Response `409 Conflict`

```json
{
  "code": "CLIENT_CONFLICT",
  "message": "The client was modified by another user.",
  "resourceId": 20,
  "currentVersion": 3,
  "updatedBy": "alice",
  "updatedAt": "2026-04-16T11:30:00Z",
  "fieldConflicts": []
}
```

## Error Taxonomy

| Code | Meaning | HTTP Status |
| --- | --- | --- |
| `INVALID_REQUEST` | Request shape or value invalid | `400` |
| `WORK_ITEM_NOT_FOUND` | Referenced work item does not exist | `404` |
| `CLIENT_NOT_FOUND` | Referenced client does not exist | `404` |
| `WORK_ITEM_CONFLICT` | Version mismatch on work item update | `409` |
| `CLIENT_CONFLICT` | Version mismatch on client update | `409` |
| `CLIENT_POLICY_VIOLATION` | Client tier, type, or status blocked the action | `422` |
| `WORK_ITEM_POLICY_VIOLATION` | Work item transition blocked by domain rule | `422` |
| `BULK_IMPORT_PARTIAL_FAILURE` | Import completed with row failures | `200` |

## Audit Coverage

The following fields must generate audit records when changed:

- `status`
- `dueDate`
- `assignee`
- `memo`
- `tags`
- `clientId`

The following fields are not audited directly:

- `updatedAt`
- `version`

## Implementation Notes

- Listing and export must share filter parsing code
- Listing and audit history must use separate query paths
- Mutation handlers must apply policy validation before persistence commit
- Conflict responses must preserve enough state for client-side merge UI
