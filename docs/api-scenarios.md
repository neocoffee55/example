# API Scenarios

## Scenario 1: List WorkItems With Filters And Sorting

### Request

`GET /api/work-items?clientName=han&statuses=TODO,HOLD&assignees=kim&dueDateFrom=2026-03-01&dueDateTo=2026-03-31&sort=dueDate:asc,clientName:asc&pageSize=50&cursor=opaque-token`

### Expected Behavior

- Returns the current page of work items
- Applies filters on client name, status, assignee, and due date
- Uses the same filter semantics later reused by export
- Does not include audit history payload in the listing response

### Response Draft

```json
{
  "items": [
    {
      "id": 101,
      "clientId": 20,
      "clientName": "Hanbit Tax",
      "type": "FILING",
      "status": "TODO",
      "assignee": "kim",
      "dueDate": "2026-03-20",
      "updatedAt": "2026-04-16T11:22:00Z",
      "version": 4,
      "tier": "VIP",
      "clientType": "CORPORATE"
    }
  ],
  "page": {
    "nextCursor": "opaque-token-2",
    "pageSize": 50,
    "hasNext": true
  }
}
```

## Scenario 2: Inline Status Update Success

### Request

`PATCH /api/work-items/101`

```json
{
  "version": 4,
  "operations": [
    {
      "field": "status",
      "value": "IN_PROGRESS"
    }
  ]
}
```

### Expected Behavior

- Version matches persisted row
- Domain policy validates requested change
- Work item is updated
- Audit log entry is created for `status`

### Response Draft

```json
{
  "id": 101,
  "status": "IN_PROGRESS",
  "dueDate": "2026-03-20",
  "version": 5,
  "updatedAt": "2026-04-16T11:25:00Z"
}
```

## Scenario 3: Inline Update Conflict

### Request

`PATCH /api/work-items/101`

```json
{
  "version": 4,
  "operations": [
    {
      "field": "dueDate",
      "value": "2026-03-25"
    }
  ]
}
```

### Expected Behavior

- Persisted version is no longer `4`
- Server rejects with `409 Conflict`
- Response includes enough information for UI comparison and merge

### Response Draft

```json
{
  "code": "WORK_ITEM_CONFLICT",
  "message": "The work item was modified by another user.",
  "workItemId": 101,
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

## Scenario 4: Create WorkItem Rejected By Client Policy

### Request

`POST /api/work-items`

```json
{
  "clientId": 20,
  "type": "FILING",
  "status": "TODO",
  "assignee": null,
  "dueDate": "2026-03-20",
  "tags": ["march"],
  "memo": "priority filing"
}
```

### Expected Behavior

- The related client is `VIP`
- Policy requires an assignee
- Server rejects with validation or policy error

### Response Draft

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

## Scenario 5: Bulk Import Partial Success

### Request

`POST /api/work-items/bulk-import`

```json
{
  "requestId": "import-2026-04-16-001",
  "items": [
    {
      "clientId": 20,
      "type": "FILING",
      "status": "TODO",
      "assignee": "kim",
      "dueDate": "2026-03-20"
    },
    {
      "clientId": 99,
      "type": "FILING",
      "status": "TODO",
      "assignee": "lee",
      "dueDate": "2026-03-20"
    }
  ]
}
```

### Expected Behavior

- Valid rows are persisted
- Invalid rows are reported with row-level errors
- Request can be retried safely if idempotency support is added

### Response Draft

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

## Scenario 6: Export Current Filtered Result Set

### Request

`GET /api/work-items/export?clientName=han&statuses=TODO,HOLD&assignees=kim&dueDateFrom=2026-03-01&dueDateTo=2026-03-31&sort=dueDate:asc,clientName:asc`

### Expected Behavior

- Reuses the same filter contract as listing
- Streams CSV response instead of materializing full result in memory
- Returns only the currently filtered dataset

### Response Headers Draft

```text
Content-Type: text/csv
Content-Disposition: attachment; filename="work-items-2026-04-16.csv"
Transfer-Encoding: chunked
```

## Scenario 7: View Audit History For A WorkItem

### Request

`GET /api/work-items/101/audit-logs?pageSize=50&cursor=opaque-token`

### Expected Behavior

- Returns field-level audit entries for one work item
- Ordered by newest first
- Separate query path from listing

### Response Draft

```json
{
  "items": [
    {
      "auditLogId": 8001,
      "workItemId": 101,
      "fieldName": "status",
      "beforeValue": "TODO",
      "afterValue": "IN_PROGRESS",
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

## Scenario 8: Change Client Status

### Request

`PATCH /api/clients/20`

```json
{
  "version": 2,
  "status": "INACTIVE"
}
```

### Expected Behavior

- Client status is updated
- Future work item creation for that client is blocked immediately
- Optional downstream compliance process may mark open work items for attention

### Response Draft

```json
{
  "id": 20,
  "status": "INACTIVE",
  "version": 3,
  "updatedAt": "2026-04-16T11:30:00Z"
}
```

## Error Taxonomy Draft

| Code | Meaning | HTTP Status |
| --- | --- | --- |
| `WORK_ITEM_CONFLICT` | Version mismatch on mutable work item | `409` |
| `CLIENT_POLICY_VIOLATION` | Client tier/type/status policy blocked action | `422` |
| `CLIENT_NOT_FOUND` | Referenced client does not exist | `404` or `422` |
| `WORK_ITEM_NOT_FOUND` | Referenced work item does not exist | `404` |
| `INVALID_REQUEST` | Request shape or value invalid | `400` |
| `BULK_IMPORT_PARTIAL_FAILURE` | Import completed with row failures | `200` or `207` |
