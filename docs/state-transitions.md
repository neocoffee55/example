# State Transitions

## WorkItem Status State Machine

### States

- `TODO`
- `IN_PROGRESS`
- `DONE`
- `HOLD`

### Allowed Transitions

| From | To | Allowed | Notes |
| --- | --- | --- | --- |
| TODO | IN_PROGRESS | Yes | Normal start of work |
| TODO | HOLD | Yes | Requires hold reason if implemented |
| TODO | DONE | Conditional | Allow only if business flow accepts direct completion |
| IN_PROGRESS | TODO | Conditional | Allow only if rollback is meaningful in the business |
| IN_PROGRESS | HOLD | Yes | Preserve reason/context |
| IN_PROGRESS | DONE | Yes | Normal completion |
| HOLD | TODO | Yes | Resume after hold cleared |
| HOLD | IN_PROGRESS | Yes | Resume directly into active work |
| HOLD | DONE | Conditional | Allow only if hold reason no longer matters |
| DONE | TODO | Conditional | Reopen flow should be explicit |
| DONE | IN_PROGRESS | Conditional | Reopen flow should be explicit |
| DONE | HOLD | Conditional | Rare, should likely require explicit note |

## Recommended Transition Policy

- Allow `TODO -> IN_PROGRESS`, `TODO -> HOLD`, `IN_PROGRESS -> HOLD`, `IN_PROGRESS -> DONE`, `HOLD -> TODO`, `HOLD -> IN_PROGRESS`
- Treat every transition out of `DONE` as a reopen action with extra audit detail
- Avoid silent direct `TODO -> DONE` unless the domain team confirms it reflects actual operations

## Client Status State Machine

### States

- `ACTIVE`
- `INACTIVE`

### Allowed Transitions

| From | To | Allowed | Notes |
| --- | --- | --- | --- |
| ACTIVE | INACTIVE | Yes | Future work item creation blocked immediately |
| INACTIVE | ACTIVE | Yes | Future work item creation allowed immediately |

## Tier Transition Notes

| From | To | Impact |
| --- | --- | --- |
| BASIC | PREMIUM | Mostly prioritization and defaults |
| PREMIUM | VIP | Assignee and stricter validation may become mandatory |
| VIP | PREMIUM | Hard VIP restrictions relaxed |
| Any | Same | No-op |

## Concurrency State Rules

- Every mutable `WorkItem` carries a `version`
- Transition requests must include the client-observed `version`
- If stored version differs, the transition is rejected with a conflict response
- Audit records are written only for committed transitions

## Conflict Resolution States

This is not a persisted entity state machine, but a UI workflow contract.

| UI State | Meaning |
| --- | --- |
| Clean | Local value matches server baseline |
| Dirty | User changed local value but has not saved |
| Saving | Request in flight |
| Conflict | Server rejected due to version mismatch |
| Resolved | User selected current value, attempted value, or merged result |
| Saved | Conflict-free update committed |

## Recommended Invariants

- A transition must not bypass version checking
- A transition must not ignore client-level policy validation
- A transition to `DONE` should stamp a meaningful audit event
- Reopen transitions should be distinguishable from normal edits in audit logs
