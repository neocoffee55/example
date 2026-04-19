# Product Context

## Problem Statement

Tax professionals need a workbench that handles the peak filing season where one person may manage 50 to 200 clients at once. The system must outperform spreadsheet workflows by supporting fast, keyboard-first editing, reliable concurrent updates, and scalable handling of more than 100,000 work items over time.

## Primary Users

- Tax accountant managing many clients during filing season
- Team lead overseeing assignment and workload distribution
- Operations/admin staff importing bulk work items and exporting filtered data

## Core User Jobs

- Review a large list of work items without losing context
- Filter and sort by client, status, assignee, and due date
- Edit status and due date inline with minimal pointer usage
- Create a work item and assign it to a client quickly
- Import thousands of work items safely
- Export the current filtered result set as CSV
- Inspect field-level audit history for a specific work item

## Product Constraints

- Must remain usable at 100,000+ work items
- Must tolerate concurrent editing by multiple users
- Must preserve integrity across Client and WorkItem business rules
- Must stream bulk import and export paths safely
- Must expose field-level audit history without degrading list performance

## Non-Goals For Initial Delivery

- Full role-based access control
- Real-time collaborative cursors/presence
- Notifications, email, or external workflow orchestration
- Rich spreadsheet formulas or pivot-style analytics

## Success Criteria

- Listing remains responsive under large datasets
- Inline edits surface conflicts explicitly instead of silently overwriting
- Bulk insert and export complete without loading entire datasets into memory
- Audit trails are queryable per field and per work item
- README clearly explains architectural reasoning and trade-offs
