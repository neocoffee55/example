import { FormEvent, KeyboardEvent, useEffect, useMemo, useState } from "react";

type WorkItemStatus = "TODO" | "IN_PROGRESS" | "DONE" | "HOLD";
type WorkItemType = "FILING" | "BOOKKEEPING" | "REVIEW" | "ETC";
type ClientTier = "BASIC" | "PREMIUM" | "VIP";
type ClientType = "INDIVIDUAL" | "CORPORATE";

type WorkItem = {
  id: number;
  clientId: number;
  clientName: string;
  bizNo: string;
  type: WorkItemType;
  status: WorkItemStatus;
  assignee: string | null;
  dueDate: string;
  tags: string[];
  memo: string | null;
  updatedAt: string;
  version: number;
  clientType: ClientType;
  clientTier: ClientTier;
  clientStatus: "ACTIVE" | "INACTIVE";
};

type PageEnvelope<T> = {
  items: T[];
  page: {
    nextCursor: string | null;
    pageSize: number;
    hasNext: boolean;
  };
};

type AuditLog = {
  auditLogId: number;
  workItemId: number;
  fieldName: string;
  beforeValue: string | null;
  afterValue: string | null;
  actorName: string;
  source: string;
  changedAt: string;
};

type ConflictResponse = {
  code: string;
  message: string;
  resourceId: number;
  currentVersion: number;
  updatedBy: string;
  updatedAt: string;
  fieldConflicts: Array<{
    field: string;
    baseValue: string | null;
    attemptedValue: string | null;
    currentValue: string | null;
  }>;
};

type Filters = {
  clientName: string;
  statuses: WorkItemStatus[];
  assignees: string;
};

const STATUS_OPTIONS: WorkItemStatus[] = ["TODO", "IN_PROGRESS", "DONE", "HOLD"];

const EMPTY_FILTERS: Filters = {
  clientName: "",
  statuses: [],
  assignees: "",
};

const API_BASE = "/api";

function App() {
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS);
  const [workItems, setWorkItems] = useState<WorkItem[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [auditItems, setAuditItems] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [auditLoading, setAuditLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [conflict, setConflict] = useState<ConflictResponse | null>(null);
  const [bulkInput, setBulkInput] = useState<string>(
    JSON.stringify(
      {
        requestId: "import-demo-001",
        items: [
          {
            clientId: 20,
            type: "FILING",
            status: "TODO",
            assignee: "kim",
            dueDate: "2026-03-31",
            tags: ["march"],
            memo: "imported from frontend",
          },
        ],
      },
      null,
      2,
    ),
  );

  const selectedWorkItem = useMemo(
    () => workItems.find((item) => item.id === selectedId) ?? null,
    [workItems, selectedId],
  );

  useEffect(() => {
    void loadWorkItems();
  }, []);

  useEffect(() => {
    if (selectedId == null) {
      setAuditItems([]);
      return;
    }
    void loadAuditLogs(selectedId);
  }, [selectedId]);

  async function loadWorkItems(cursor?: string) {
    setLoading(true);
    setError(null);
    try {
      const query = new URLSearchParams();
      if (filters.clientName) query.set("clientName", filters.clientName);
      if (filters.statuses.length > 0) query.set("statuses", filters.statuses.join(","));
      if (filters.assignees) query.set("assignees", filters.assignees);
      query.set("pageSize", "25");
      if (cursor) query.set("cursor", cursor);

      const response = await fetch(`${API_BASE}/work-items?${query.toString()}`);
      if (!response.ok) {
        throw new Error(`Failed to load work items (${response.status})`);
      }
      const payload = (await response.json()) as PageEnvelope<WorkItem>;
      setWorkItems(cursor ? [...workItems, ...payload.items] : payload.items);
      setNextCursor(payload.page.nextCursor);
      if (!selectedId && payload.items.length > 0) {
        setSelectedId(payload.items[0].id);
      }
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  }

  async function loadAuditLogs(id: number) {
    setAuditLoading(true);
    try {
      const response = await fetch(`${API_BASE}/work-items/${id}/audit-logs?pageSize=20`);
      if (!response.ok) {
        throw new Error(`Failed to load audit history (${response.status})`);
      }
      const payload = (await response.json()) as PageEnvelope<AuditLog>;
      setAuditItems(payload.items);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Unknown error");
    } finally {
      setAuditLoading(false);
    }
  }

  async function applyInlinePatch(
    item: WorkItem,
    field: "status" | "dueDate",
    value: string,
  ) {
    setConflict(null);
    const payload = {
      version: item.version,
      operations: [
        {
          field,
          baseValue: field === "status" ? item.status : item.dueDate,
          value,
        },
      ],
    };

    const response = await fetch(`${API_BASE}/work-items/${item.id}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      const updated = (await response.json()) as WorkItem;
      setWorkItems((current) => current.map((entry) => (entry.id === item.id ? updated : entry)));
      if (selectedId === item.id) {
        void loadAuditLogs(item.id);
      }
      return;
    }

    if (response.status === 409) {
      const conflictPayload = (await response.json()) as ConflictResponse;
      setConflict(conflictPayload);
      return;
    }

    const errorPayload = await response.json().catch(() => null);
    throw new Error(errorPayload?.message ?? `Patch failed (${response.status})`);
  }

  async function submitBulkImport(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      const response = await fetch(`${API_BASE}/work-items/bulk-import`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: bulkInput,
      });
      if (!response.ok) {
        throw new Error(`Bulk import failed (${response.status})`);
      }
      await loadWorkItems();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Unknown error");
    }
  }

  function onFilterSubmit(event: FormEvent) {
    event.preventDefault();
    void loadWorkItems();
  }

  function onStatusToggle(status: WorkItemStatus) {
    setFilters((current) => ({
      ...current,
      statuses: current.statuses.includes(status)
        ? current.statuses.filter((candidate) => candidate !== status)
        : [...current.statuses, status],
    }));
  }

  function onKeyNavigate(event: KeyboardEvent<HTMLDivElement>, index: number) {
    if (event.key === "ArrowDown" && workItems[index + 1]) {
      setSelectedId(workItems[index + 1].id);
    }
    if (event.key === "ArrowUp" && workItems[index - 1]) {
      setSelectedId(workItems[index - 1].id);
    }
  }

  return (
    <div className="min-h-screen bg-paper text-ink">
      <div className="mx-auto flex max-w-[1600px] flex-col gap-6 px-4 py-6 lg:px-8">
        <header className="rounded-[28px] border border-white/80 bg-white/80 p-6 shadow-frame backdrop-blur">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-sm uppercase tracking-[0.3em] text-ocean">Tax Workbench</p>
              <h1 className="font-display text-4xl leading-tight text-ink">
                Filing season should feel like triage, not spreadsheet drift.
              </h1>
              <p className="mt-3 max-w-3xl text-sm text-slate-600">
                Keyboard-first workbench for filtering, inline editing, audit review, export,
                and bulk import against the Spring API slice.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-3 text-sm lg:w-[360px]">
              <Metric label="Rows loaded" value={String(workItems.length)} />
              <Metric label="Selected work" value={selectedId ? `#${selectedId}` : "none"} />
              <Metric label="Next cursor" value={nextCursor ? "ready" : "end"} />
              <Metric label="Conflict mode" value={conflict ? "active" : "clear"} />
            </div>
          </div>
        </header>

        {error && (
          <section className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </section>
        )}

        {conflict && (
          <section className="rounded-3xl border border-amber-300 bg-amber-50 p-5 shadow-sm">
            <div className="flex flex-col gap-2">
              <strong className="text-amber-900">Conflict detected on work item #{conflict.resourceId}</strong>
              <span className="text-sm text-amber-800">
                Updated by {conflict.updatedBy} at {new Date(conflict.updatedAt).toLocaleString()}.
              </span>
              <div className="grid gap-2 text-sm">
                {conflict.fieldConflicts.map((entry) => (
                  <div key={entry.field} className="rounded-2xl bg-white px-4 py-3">
                    <div className="font-semibold text-ink">{entry.field}</div>
                    <div className="text-slate-600">base: {entry.baseValue ?? "null"}</div>
                    <div className="text-slate-600">attempted: {entry.attemptedValue ?? "null"}</div>
                    <div className="text-slate-600">current: {entry.currentValue ?? "null"}</div>
                  </div>
                ))}
              </div>
            </div>
          </section>
        )}

        <div className="grid gap-6 xl:grid-cols-[1.8fr_1fr]">
          <main className="flex flex-col gap-6">
            <section className="rounded-[28px] border border-white/80 bg-white/80 p-5 shadow-frame backdrop-blur">
              <form className="grid gap-4 lg:grid-cols-[1.4fr_1fr_auto]" onSubmit={onFilterSubmit}>
                <label className="flex flex-col gap-2 text-sm">
                  Client name
                  <input
                    className="rounded-2xl border border-slate-200 bg-white px-4 py-3"
                    value={filters.clientName}
                    onChange={(event) =>
                      setFilters((current) => ({ ...current, clientName: event.target.value }))
                    }
                    placeholder="Hanbit"
                  />
                </label>
                <label className="flex flex-col gap-2 text-sm">
                  Assignee
                  <input
                    className="rounded-2xl border border-slate-200 bg-white px-4 py-3"
                    value={filters.assignees}
                    onChange={(event) =>
                      setFilters((current) => ({ ...current, assignees: event.target.value }))
                    }
                    placeholder="kim"
                  />
                </label>
                <div className="flex items-end">
                  <button
                    type="submit"
                    className="w-full rounded-2xl bg-accent px-4 py-3 font-semibold text-white transition hover:bg-[#8c4027]"
                  >
                    Apply filters
                  </button>
                </div>
              </form>

              <div className="mt-4 flex flex-wrap gap-2">
                {STATUS_OPTIONS.map((status) => {
                  const active = filters.statuses.includes(status);
                  return (
                    <button
                      key={status}
                      type="button"
                      className={`rounded-full px-4 py-2 text-sm transition ${
                        active ? "bg-ocean text-white" : "bg-slate-100 text-slate-600"
                      }`}
                      onClick={() => onStatusToggle(status)}
                    >
                      {status}
                    </button>
                  );
                })}
                <a
                  className="ml-auto rounded-full bg-moss px-4 py-2 text-sm font-semibold text-white"
                  href={`${API_BASE}/work-items/export?clientName=${encodeURIComponent(filters.clientName)}`}
                >
                  Export current slice
                </a>
              </div>
            </section>

            <section className="rounded-[28px] border border-white/80 bg-white/80 p-5 shadow-frame backdrop-blur">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="font-display text-2xl">Workbench grid</h2>
                <span className="text-sm text-slate-500">
                  Use arrow keys after selecting a row
                </span>
              </div>
              <div className="overflow-hidden rounded-3xl border border-slate-200">
                <div className="grid grid-cols-[0.8fr_1.6fr_1fr_1fr_1fr_1fr] bg-ink px-4 py-3 text-xs uppercase tracking-[0.2em] text-white">
                  <span>ID</span>
                  <span>Client</span>
                  <span>Status</span>
                  <span>Due Date</span>
                  <span>Assignee</span>
                  <span>Tier</span>
                </div>
                <div className="max-h-[560px] overflow-y-auto">
                  {loading ? (
                    <div className="p-8 text-sm text-slate-500">Loading work items...</div>
                  ) : workItems.length === 0 ? (
                    <div className="p-8 text-sm text-slate-500">No work items found.</div>
                  ) : (
                    workItems.map((item, index) => {
                      const selected = item.id === selectedId;
                      return (
                        <div
                          key={item.id}
                          className={`grid grid-cols-[0.8fr_1.6fr_1fr_1fr_1fr_1fr] gap-3 border-t border-slate-100 px-4 py-3 outline-none transition ${
                            selected ? "bg-[#fff3e8]" : "bg-white"
                          }`}
                          onClick={() => setSelectedId(item.id)}
                          onKeyDown={(event) => onKeyNavigate(event, index)}
                          role="button"
                          tabIndex={0}
                        >
                          <span className="text-sm font-semibold text-slate-700">#{item.id}</span>
                          <div className="flex flex-col">
                            <span className="font-semibold text-ink">{item.clientName}</span>
                            <span className="text-xs text-slate-500">{item.type}</span>
                          </div>
                          <select
                            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm"
                            value={item.status}
                            onChange={(event) => void applyInlinePatch(item, "status", event.target.value)}
                          >
                            {STATUS_OPTIONS.map((status) => (
                              <option key={status} value={status}>
                                {status}
                              </option>
                            ))}
                          </select>
                          <input
                            type="date"
                            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm"
                            value={item.dueDate}
                            onChange={(event) => void applyInlinePatch(item, "dueDate", event.target.value)}
                          />
                          <span className="self-center text-sm text-slate-600">
                            {item.assignee ?? "unassigned"}
                          </span>
                          <span
                            className={`self-center rounded-full px-3 py-1 text-xs font-semibold ${
                              item.clientTier === "VIP"
                                ? "bg-accent text-white"
                                : item.clientTier === "PREMIUM"
                                  ? "bg-ocean text-white"
                                  : "bg-slate-100 text-slate-600"
                            }`}
                          >
                            {item.clientTier}
                          </span>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>

              {nextCursor && (
                <div className="mt-4 flex justify-end">
                  <button
                    type="button"
                    className="rounded-2xl border border-slate-300 px-4 py-3 text-sm font-semibold text-ink"
                    onClick={() => void loadWorkItems(nextCursor)}
                  >
                    Load next page
                  </button>
                </div>
              )}
            </section>
          </main>

          <aside className="flex flex-col gap-6">
            <section className="rounded-[28px] border border-white/80 bg-white/80 p-5 shadow-frame backdrop-blur">
              <div className="mb-3">
                <h2 className="font-display text-2xl">Audit detail</h2>
                <p className="text-sm text-slate-500">
                  {selectedWorkItem
                    ? `Showing change trail for #${selectedWorkItem.id}`
                    : "Select a row to inspect field history."}
                </p>
              </div>
              {auditLoading ? (
                <div className="text-sm text-slate-500">Loading audit history...</div>
              ) : (
                <div className="grid gap-3">
                  {auditItems.map((entry) => (
                    <div key={entry.auditLogId} className="rounded-2xl bg-slate-50 p-4 text-sm">
                      <div className="flex items-center justify-between">
                        <strong>{entry.fieldName}</strong>
                        <span className="text-xs uppercase tracking-[0.2em] text-slate-400">
                          {entry.source}
                        </span>
                      </div>
                      <div className="mt-2 text-slate-600">before: {entry.beforeValue ?? "null"}</div>
                      <div className="text-slate-600">after: {entry.afterValue ?? "null"}</div>
                      <div className="mt-2 text-xs text-slate-500">
                        {entry.actorName} · {new Date(entry.changedAt).toLocaleString()}
                      </div>
                    </div>
                  ))}
                  {auditItems.length === 0 && (
                    <div className="text-sm text-slate-500">No audit entries for the selected row.</div>
                  )}
                </div>
              )}
            </section>

            <section className="rounded-[28px] border border-white/80 bg-white/80 p-5 shadow-frame backdrop-blur">
              <div className="mb-3">
                <h2 className="font-display text-2xl">Bulk import</h2>
                <p className="text-sm text-slate-500">
                  Paste JSON matching the backend bulk import contract.
                </p>
              </div>
              <form className="grid gap-3" onSubmit={submitBulkImport}>
                <textarea
                  className="min-h-[220px] rounded-2xl border border-slate-200 bg-white px-4 py-3 text-xs text-slate-700"
                  value={bulkInput}
                  onChange={(event) => setBulkInput(event.target.value)}
                />
                <button
                  type="submit"
                  className="rounded-2xl bg-ink px-4 py-3 font-semibold text-white transition hover:bg-[#0e1b2c]"
                >
                  Send bulk import
                </button>
              </form>
            </section>
          </aside>
        </div>
      </div>
    </div>
  );
}

function Metric(props: { label: string; value: string }) {
  return (
    <div className="rounded-2xl bg-slate-50 px-4 py-3">
      <div className="text-[11px] uppercase tracking-[0.2em] text-slate-400">{props.label}</div>
      <div className="mt-2 text-lg font-semibold text-ink">{props.value}</div>
    </div>
  );
}

export default App;
