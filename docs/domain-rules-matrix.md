# Domain Rules Matrix

## Client To WorkItem Rules

| Client Status | Client Tier | Client Type | Operation | Allowed | Rule |
| --- | --- | --- | --- | --- | --- |
| ACTIVE | BASIC | INDIVIDUAL | Create WorkItem | Yes | Standard validation only |
| ACTIVE | BASIC | CORPORATE | Create WorkItem | Yes | Must use corporate-compatible work type |
| ACTIVE | PREMIUM | INDIVIDUAL | Create WorkItem | Yes | Standard validation plus premium defaults |
| ACTIVE | PREMIUM | CORPORATE | Create WorkItem | Yes | Standard validation plus premium defaults |
| ACTIVE | VIP | INDIVIDUAL | Create WorkItem | Conditional | Assignee is mandatory |
| ACTIVE | VIP | CORPORATE | Create WorkItem | Conditional | Assignee is mandatory and work type must be explicitly chosen |
| INACTIVE | BASIC | INDIVIDUAL | Create WorkItem | No | Inactive clients cannot receive new work |
| INACTIVE | BASIC | CORPORATE | Create WorkItem | No | Inactive clients cannot receive new work |
| INACTIVE | PREMIUM | INDIVIDUAL | Create WorkItem | No | Inactive clients cannot receive new work |
| INACTIVE | PREMIUM | CORPORATE | Create WorkItem | No | Inactive clients cannot receive new work |
| INACTIVE | VIP | INDIVIDUAL | Create WorkItem | No | Inactive clients cannot receive new work |
| INACTIVE | VIP | CORPORATE | Create WorkItem | No | Inactive clients cannot receive new work |

## WorkItem Update Rules

| Condition | Operation | Allowed | Rule |
| --- | --- | --- | --- |
| WorkItem exists and version matches | Inline status update | Yes | Record audit log |
| WorkItem exists and version matches | Inline due date update | Yes | Record audit log |
| WorkItem version mismatch | Any mutable field update | No | Return conflict payload |
| Related client is INACTIVE | Reassign or create derived work | No | Block until client reactivated |
| Related client is VIP and assignee is empty | Save mutable fields | No | Assignee required |
| WorkItem status is DONE | Update due date | Conditional | Either block or require explicit override policy |
| WorkItem status is HOLD | Set status to DONE | Conditional | Reason for release may be required |

## Client Change Impact Rules

| Client Change | Impact On Existing WorkItems | Processing Strategy |
| --- | --- | --- |
| `status: ACTIVE -> INACTIVE` | Open work items become non-creatable for derivatives and may become read-only | Synchronous validation on future writes, optional async marking |
| `status: INACTIVE -> ACTIVE` | New work item creation allowed again | Immediate |
| `tier: PREMIUM -> VIP` | Existing open work items may now require assignee | Detect on next write and optionally run background compliance check |
| `tier: VIP -> BASIC` | VIP-only restrictions relaxed | Immediate for future writes |
| `type: INDIVIDUAL -> CORPORATE` | Some work item types may become invalid for future creation | Immediate for future writes, no silent mutation of existing rows |

## Defaulting Rules

| Client Type | Suggested Default Work Types | Due Date Policy |
| --- | --- | --- |
| INDIVIDUAL | `FILING`, `BOOKKEEPING`, `ETC` | User-provided unless policy derives from filing calendar |
| CORPORATE | `FILING`, `REVIEW`, `BOOKKEEPING` | May use stricter default date windows |

| Client Tier | Default UI/Workflow Behavior |
| --- | --- |
| BASIC | Standard queue treatment |
| PREMIUM | Prefer surfaced priority and richer warnings |
| VIP | Highlighted row state, mandatory assignee, stricter validation |

## Audit Rules

| Field | Audit Required | Notes |
| --- | --- | --- |
| `status` | Yes | Must capture before and after value |
| `dueDate` | Yes | Must capture before and after value |
| `assignee` | Yes | Important for VIP enforcement |
| `memo` | Yes | May be truncated in list view but not in audit record |
| `tags` | Yes | Store normalized before and after representation |
| `clientId` | Yes | High-risk change |
| `updatedAt` | No | System-maintained field |
| `version` | No | Technical concurrency field |

## Bulk Import Rules

| Condition | Result |
| --- | --- |
| Unknown client reference | Reject row with validation error |
| Inactive client target | Reject row with policy error |
| VIP client without assignee | Reject row with policy error |
| Duplicate import row within same request | Reject duplicate row or collapse by idempotency policy |
| Mixed valid and invalid rows | Partial success allowed with row-level failure report |

## Open Decisions

- Whether `DONE -> IN_PROGRESS` is allowed without elevated workflow intent
- Whether inactive-client linked work items become automatically `HOLD`
- Whether premium tier has any hard validation or only UI priority treatment
