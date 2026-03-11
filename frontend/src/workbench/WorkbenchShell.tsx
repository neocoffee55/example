import { useEffect, useMemo, useRef, useState } from "react";
import {
  flexRender,
  getCoreRowModel,
  useReactTable,
  type ColumnDef
} from "@tanstack/react-table";

type WorkbenchRow = {
  id: string;
  revision: number;
  client: string;
  bizNo: string;
  workType: string;
  status: string;
  assignee: string;
  dueDate: string;
  updatedAt: string;
};

type ClientRow = {
  id: string;
  name: string;
  bizNo: string;
  type: string;
  status: string;
  tier: string;
  updatedAt: string;
};

type AuditLogRow = {
  workItemId: string;
  revision: number;
  changedAt: string;
  changedBy: string;
  fieldName: string;
  beforeValue: string;
  afterValue: string;
};

type EditableColumnKey = Exclude<keyof WorkbenchRow, "id">;
type ClientEditableColumnKey = Exclude<keyof ClientRow, "id">;

type WorkbenchColumn = {
  key: EditableColumnKey;
  label: string;
};

type SortDirection = "asc" | "desc";
type SortKey = EditableColumnKey;

type EditingCell = {
  rowId: string;
  columnKey: EditableColumnKey;
};

type ClientEditingCell = {
  rowId: string;
  columnKey: ClientEditableColumnKey;
};

type SaveSummary = {
  added: number;
  updated: number;
  deleted: number;
};

type FilterState = {
  client: string;
  status: string;
  assignee: string;
  dueDate: string;
};

type ClientFilterState = {
  keyword: string;
};

type ToastState = {
  message: string;
  visible: boolean;
};

const initialRows: WorkbenchRow[] = [
  {
    id: "WI-10031",
    revision: 0,
    client: "Han River Holdings",
    bizNo: "123-45-67890",
    workType: "FILING",
    status: "IN_PROGRESS",
    assignee: "insu",
    dueDate: "2026-03-20",
    updatedAt: "09:40"
  },
  {
    id: "WI-10032",
    revision: 0,
    client: "Mirae Clinic",
    bizNo: "220-11-90876",
    workType: "REVIEW",
    status: "HOLD",
    assignee: "jane",
    dueDate: "2026-03-18",
    updatedAt: "09:12"
  }
];

const initialClients: ClientRow[] = [
  {
    id: "CL-1001",
    name: "Han River Holdings",
    bizNo: "123-45-67890",
    type: "CORPORATE",
    status: "ACTIVE",
    tier: "VIP",
    updatedAt: "09:40"
  },
  {
    id: "CL-1002",
    name: "Mirae Clinic",
    bizNo: "220-11-90876",
    type: "CORPORATE",
    status: "ACTIVE",
    tier: "PREMIUM",
    updatedAt: "09:12"
  }
];

const actionButtons = ["find", "add", "del", "save", "Export"] as const;
const workTypeOptions = ["FILING", "BOOKKEEPING", "REVIEW", "ETC"] as const;
const statusOptions = ["TODO", "IN_PROGRESS", "DONE", "HOLD"] as const;
const clientTypeOptions = ["CORPORATE", "INDIVIDUAL"] as const;
const clientStatusOptions = ["ACTIVE", "INACTIVE"] as const;
const clientTierOptions = ["BASIC", "PREMIUM", "VIP"] as const;
const apiBaseUrl = "/api";

const trackedFields: EditableColumnKey[] = [
  "client",
  "bizNo",
  "workType",
  "status",
  "assignee",
  "dueDate",
  "updatedAt"
];

const trackedClientFields: ClientEditableColumnKey[] = [
  "name",
  "bizNo",
  "type",
  "status",
  "tier",
  "updatedAt"
];

const WORK_ITEM_PAGE_SIZE = 5;
const AUDIT_LOG_PAGE_SIZE = 8;

const auditFieldLabels: Record<string, string> = {
  client: "업체명",
  bizNo: "사업자번호",
  workType: "업무유형",
  status: "상태",
  assignee: "담당자",
  dueDate: "마감일",
  updatedAt: "최근수정"
};

export function WorkbenchShell() {
  const [rows, setRows] = useState<WorkbenchRow[]>(initialRows);
  const [persistedRows, setPersistedRows] = useState<WorkbenchRow[]>(initialRows);
  const [selectedRowIds, setSelectedRowIds] = useState<string[]>([]);
  const [activeWorkItemId, setActiveWorkItemId] = useState<string | null>(null);
  const [auditLogs, setAuditLogs] = useState<AuditLogRow[]>([]);
  const [sortKey, setSortKey] = useState<SortKey>("client");
  const [sortDirection, setSortDirection] = useState<SortDirection>("asc");
  const [editingCell, setEditingCell] = useState<EditingCell | null>(null);
  const [pendingFocusRowId, setPendingFocusRowId] = useState<string | null>(null);
  const [saveSummary, setSaveSummary] = useState<SaveSummary | null>(null);
  const [showClientModal, setShowClientModal] = useState(false);
  const [pendingClientAssignmentRowId, setPendingClientAssignmentRowId] = useState<string | null>(null);
  const [showStatusDropdown, setShowStatusDropdown] = useState(false);
  const [showClientInput, setShowClientInput] = useState(false);
  const [showAssigneeInput, setShowAssigneeInput] = useState(false);
  const [showDueDateCalendar, setShowDueDateCalendar] = useState(false);
  const [draftFilters, setDraftFilters] = useState<FilterState>({
    client: "",
    status: "",
    assignee: "",
    dueDate: ""
  });
  const [appliedFilters, setAppliedFilters] = useState<FilterState>({
    client: "",
    status: "",
    assignee: "",
    dueDate: ""
  });
  const [clientRows, setClientRows] = useState<ClientRow[]>(initialClients);
  const [persistedClientRows, setPersistedClientRows] = useState<ClientRow[]>(initialClients);
  const [selectedClientIds, setSelectedClientIds] = useState<string[]>([]);
  const [clientSortKey, setClientSortKey] = useState<ClientEditableColumnKey>("updatedAt");
  const [clientSortDirection, setClientSortDirection] = useState<SortDirection>("desc");
  const [clientEditingCell, setClientEditingCell] = useState<ClientEditingCell | null>(null);
  const [clientPendingFocusRowId, setClientPendingFocusRowId] = useState<string | null>(null);
  const [clientSaveSummary, setClientSaveSummary] = useState<SaveSummary | null>(null);
  const [clientDraftFilters, setClientDraftFilters] = useState<ClientFilterState>({ keyword: "" });
  const [clientAppliedFilters, setClientAppliedFilters] = useState<ClientFilterState>({ keyword: "" });
  const [toast, setToast] = useState<ToastState | null>(null);
  const [workItemPage, setWorkItemPage] = useState(1);
  const [auditLogPage, setAuditLogPage] = useState(1);
  const inputRefs = useRef<Record<string, HTMLInputElement | HTMLSelectElement | null>>({});
  const clientInputRefs = useRef<Record<string, HTMLInputElement | HTMLSelectElement | null>>({});
  const workItemRowRefs = useRef<Record<string, HTMLTableRowElement | null>>({});

  const formatUpdatedAt = (value: string) => {
    const parsedDate = new Date(value);

    if (Number.isNaN(parsedDate.getTime())) {
      return value;
    }

    const year = parsedDate.getFullYear();
    const month = String(parsedDate.getMonth() + 1).padStart(2, "0");
    const day = String(parsedDate.getDate()).padStart(2, "0");
    const hours = String(parsedDate.getHours()).padStart(2, "0");
    const minutes = String(parsedDate.getMinutes()).padStart(2, "0");

    return `${year}-${month}-${day} ${hours}:${minutes}`;
  };

  const todayDate = () => new Date().toISOString().slice(0, 10);

  const maskBizNo = (value: string) => {
    const digits = value.replaceAll(/\D/g, "").slice(0, 10);
    const parts = [digits.slice(0, 3), digits.slice(3, 5), digits.slice(5, 10)].filter(Boolean);
    return parts.join("-");
  };

  const hydrateWorkItems = (items: WorkbenchRow[]) =>
    items.map((item) => ({
      ...item,
      revision: item.revision ?? 0,
      updatedAt: formatUpdatedAt(item.updatedAt)
    }));

  const hydrateClients = (items: ClientRow[]) =>
    items.map((item) => ({
      ...item,
      updatedAt: formatUpdatedAt(item.updatedAt)
    }));

  const hydrateAuditLogs = (items: AuditLogRow[]) =>
    items.map((item) => ({
      ...item,
      changedAt: formatUpdatedAt(item.changedAt)
    }));

  const filteredRows = useMemo(() => {
    return rows.filter((row) => {
      const matchesClient =
        appliedFilters.client === "" ||
        row.client.toLowerCase().includes(appliedFilters.client.toLowerCase());
      const matchesStatus =
        appliedFilters.status === "" || row.status === appliedFilters.status;
      const matchesAssignee =
        appliedFilters.assignee === "" ||
        row.assignee.toLowerCase().includes(appliedFilters.assignee.toLowerCase());
      const matchesDueDate =
        appliedFilters.dueDate === "" || row.dueDate === appliedFilters.dueDate;

      return matchesClient && matchesStatus && matchesAssignee && matchesDueDate;
    });
  }, [appliedFilters, rows]);

  const sortedRows = useMemo(() => {
    const direction = sortDirection === "asc" ? 1 : -1;

    return [...filteredRows].sort((left, right) => {
      const leftValue = String(left[sortKey]);
      const rightValue = String(right[sortKey]);

      return leftValue.localeCompare(rightValue) * direction;
    });
  }, [filteredRows, sortDirection, sortKey]);

  const filteredClientRows = useMemo(() => {
    return clientRows.filter((row) => {
      if (clientAppliedFilters.keyword === "") {
        return true;
      }

      const keyword = clientAppliedFilters.keyword.toLowerCase();
      return (
        row.name.toLowerCase().includes(keyword) ||
        row.bizNo.toLowerCase().includes(keyword)
      );
    });
  }, [clientAppliedFilters.keyword, clientRows]);

  const sortedClientRows = useMemo(() => {
    const direction = clientSortDirection === "asc" ? 1 : -1;

    return [...filteredClientRows].sort((left, right) => {
      const leftValue = String(left[clientSortKey]);
      const rightValue = String(right[clientSortKey]);

      return leftValue.localeCompare(rightValue) * direction;
    });
  }, [filteredClientRows, clientSortDirection, clientSortKey]);

  const pagedWorkItemRows = useMemo(
    () =>
      sortedRows.slice(
        (workItemPage - 1) * WORK_ITEM_PAGE_SIZE,
        workItemPage * WORK_ITEM_PAGE_SIZE
      ),
    [sortedRows, workItemPage]
  );

  const pagedAuditLogs = useMemo(
    () =>
      auditLogs.slice(
        (auditLogPage - 1) * AUDIT_LOG_PAGE_SIZE,
        auditLogPage * AUDIT_LOG_PAGE_SIZE
      ),
    [auditLogs, auditLogPage]
  );

  const visibleRowIds = useMemo(
    () => pagedWorkItemRows.map((row) => row.id),
    [pagedWorkItemRows]
  );
  const visibleClientRowIds = useMemo(
    () => sortedClientRows.map((row) => row.id),
    [sortedClientRows]
  );

  const areAllRowsSelected =
    visibleRowIds.length > 0 && visibleRowIds.every((rowId) => selectedRowIds.includes(rowId));
  const areAllClientsSelected =
    visibleClientRowIds.length > 0 &&
    visibleClientRowIds.every((rowId) => selectedClientIds.includes(rowId));

  useEffect(() => {
    if (!pendingFocusRowId) {
      return;
    }

    setEditingCell({ rowId: pendingFocusRowId, columnKey: "client" });
  }, [pendingFocusRowId]);

  useEffect(() => {
    if (!editingCell) {
      return;
    }

    const refKey = `${editingCell.rowId}:${editingCell.columnKey}`;
    const targetInput = inputRefs.current[refKey];

    if (!targetInput) {
      return;
    }

    targetInput.focus();
    if (
      targetInput instanceof HTMLInputElement &&
      pendingFocusRowId === editingCell.rowId &&
      editingCell.columnKey === "client"
    ) {
      targetInput.select();
    }

    if (pendingFocusRowId === editingCell.rowId && editingCell.columnKey === "client") {
      setPendingFocusRowId(null);
    }
  }, [editingCell, pendingFocusRowId]);

  useEffect(() => {
    if (!clientPendingFocusRowId) {
      return;
    }

    setClientEditingCell({ rowId: clientPendingFocusRowId, columnKey: "name" });
  }, [clientPendingFocusRowId]);

  useEffect(() => {
    if (!clientEditingCell) {
      return;
    }

    const refKey = `${clientEditingCell.rowId}:${clientEditingCell.columnKey}`;
    const targetInput = clientInputRefs.current[refKey];

    if (!targetInput) {
      return;
    }

    targetInput.focus();
    if (
      targetInput instanceof HTMLInputElement &&
      clientPendingFocusRowId === clientEditingCell.rowId &&
      clientEditingCell.columnKey === "name"
    ) {
      targetInput.select();
    }

    if (clientPendingFocusRowId === clientEditingCell.rowId && clientEditingCell.columnKey === "name") {
      setClientPendingFocusRowId(null);
    }
  }, [clientEditingCell, clientPendingFocusRowId]);

  useEffect(() => {
    void fetchWorkItems({
      client: "",
      status: "",
      assignee: "",
      dueDate: ""
    });
    void fetchClients("");
  }, []);

  useEffect(() => {
    if (sortedRows.length === 0) {
      setActiveWorkItemId(null);
      setAuditLogs([]);
      return;
    }

    if (!activeWorkItemId || !pagedWorkItemRows.some((row) => row.id === activeWorkItemId)) {
      setActiveWorkItemId(pagedWorkItemRows[0]?.id ?? sortedRows[0].id);
    }
  }, [activeWorkItemId, pagedWorkItemRows, sortedRows]);

  useEffect(() => {
    if (!activeWorkItemId) {
      setAuditLogs([]);
      return;
    }

    void fetchAuditLogs(activeWorkItemId);
  }, [activeWorkItemId]);

  useEffect(() => {
    if (!activeWorkItemId) {
      return;
    }

    workItemRowRefs.current[activeWorkItemId]?.focus();
  }, [activeWorkItemId, sortedRows]);

  useEffect(() => {
    const totalPages = Math.max(1, Math.ceil(sortedRows.length / WORK_ITEM_PAGE_SIZE));
    setWorkItemPage((current) => Math.min(current, totalPages));
  }, [sortedRows.length]);

  useEffect(() => {
    const totalPages = Math.max(1, Math.ceil(auditLogs.length / AUDIT_LOG_PAGE_SIZE));
    setAuditLogPage((current) => Math.min(current, totalPages));
  }, [auditLogs.length]);

  useEffect(() => {
    if (!toast) {
      return;
    }

    const fadeOutTimeoutId = window.setTimeout(() => {
      setToast((current) => (current ? { ...current, visible: false } : null));
    }, 1600);

    const timeoutId = window.setTimeout(() => {
      setToast(null);
    }, 2000);

    return () => {
      window.clearTimeout(fadeOutTimeoutId);
      window.clearTimeout(timeoutId);
    };
  }, [toast]);

  const handleSort = (columnKey: SortKey) => {
    if (sortKey === columnKey) {
      setSortDirection((current) => (current === "asc" ? "desc" : "asc"));
      return;
    }

    setSortKey(columnKey);
    setSortDirection("asc");
  };

  const handleCellClick = (rowId: string, columnKey: EditableColumnKey) => {
    if (columnKey === "status" || columnKey === "client" || columnKey === "bizNo" || columnKey === "assignee") {
      return;
    }

    setEditingCell({ rowId, columnKey });
  };

  const handleCellChange = (rowId: string, columnKey: EditableColumnKey, value: string) => {
    setSaveSummary(null);
    const normalizedValue =
      columnKey === "bizNo"
        ? maskBizNo(value)
        : columnKey === "assignee"
          ? value.slice(0, 20)
          : value;

    setRows((currentRows) =>
      currentRows.map((row) =>
        row.id === rowId
          ? {
              ...row,
              [columnKey]: normalizedValue
            }
          : row
      )
    );
  };

  const handleCellCommit = () => {
    setEditingCell(null);
  };

  const handleClientSort = (columnKey: ClientEditableColumnKey) => {
    if (clientSortKey === columnKey) {
      setClientSortDirection((current) => (current === "asc" ? "desc" : "asc"));
      return;
    }

    setClientSortKey(columnKey);
    setClientSortDirection("asc");
  };

  const handleClientCellClick = (rowId: string, columnKey: ClientEditableColumnKey) => {
    setClientEditingCell({ rowId, columnKey });
  };

  const handleClientCellChange = (
    rowId: string,
    columnKey: ClientEditableColumnKey,
    value: string
  ) => {
    setClientSaveSummary(null);
    const normalizedValue = columnKey === "bizNo" ? maskBizNo(value) : value;
    setClientRows((currentRows) =>
      currentRows.map((row) =>
        row.id === rowId
          ? {
              ...row,
              [columnKey]: normalizedValue
            }
          : row
      )
    );
  };

  const handleClientCellCommit = () => {
    setClientEditingCell(null);
  };

  const handleRowSelection = (rowId: string) => {
    setSelectedRowIds((current) =>
      current.includes(rowId)
        ? current.filter((id) => id !== rowId)
        : [...current, rowId]
    );
  };

  const handleToggleAllRows = () => {
    setSelectedRowIds((current) => (areAllRowsSelected ? current.filter((id) => !visibleRowIds.includes(id)) : Array.from(new Set([...current, ...visibleRowIds]))));
  };

  const handleClientRowSelection = (rowId: string) => {
    setSelectedClientIds((current) =>
      current.includes(rowId)
        ? current.filter((id) => id !== rowId)
        : [...current, rowId]
    );
  };

  const handleToggleAllClients = () => {
    setSelectedClientIds((current) =>
      areAllClientsSelected
        ? current.filter((id) => !visibleClientRowIds.includes(id))
        : Array.from(new Set([...current, ...visibleClientRowIds]))
    );
  };

  const handleAddRow = () => {
    const nextId = `WI-${Date.now()}`;
    const newRow: WorkbenchRow = {
      id: nextId,
      revision: 0,
      client: "",
      bizNo: "",
      workType: "FILING",
      status: "TODO",
      assignee: "insu",
      dueDate: todayDate(),
      updatedAt: formatUpdatedAt(new Date().toISOString())
    };

    setSaveSummary(null);
    setRows((currentRows) => [newRow, ...currentRows]);
    setSelectedRowIds((current) => [nextId, ...current.filter((id) => id !== nextId)]);
    setPendingFocusRowId(nextId);
    setPendingClientAssignmentRowId(nextId);
    setShowClientModal(true);
  };

  const handleDeleteRows = () => {
    if (selectedRowIds.length === 0) {
      return;
    }

    setSaveSummary(null);
    setRows((currentRows) => currentRows.filter((row) => !selectedRowIds.includes(row.id)));
    setSelectedRowIds([]);
    setEditingCell(null);
  };

  const handleAddClientRow = () => {
    const nextId = `CL-${Date.now()}`;
    const newRow: ClientRow = {
      id: nextId,
      name: "",
      bizNo: "",
      type: "CORPORATE",
      status: "ACTIVE",
      tier: "BASIC",
      updatedAt: formatUpdatedAt(new Date().toISOString())
    };

    setClientSaveSummary(null);
    setClientRows((currentRows) => [newRow, ...currentRows]);
    setSelectedClientIds((current) => [nextId, ...current.filter((id) => id !== nextId)]);
    setClientPendingFocusRowId(nextId);
  };

  const handleDeleteClientRows = () => {
    if (selectedClientIds.length === 0) {
      return;
    }

    setClientSaveSummary(null);
    setClientRows((currentRows) =>
      currentRows.filter((row) => !selectedClientIds.includes(row.id))
    );
    setSelectedClientIds([]);
    setClientEditingCell(null);
  };

  const hasRowChanged = (left: WorkbenchRow, right: WorkbenchRow) =>
    trackedFields.some((field) => left[field] !== right[field]);
  const hasClientRowChanged = (left: ClientRow, right: ClientRow) =>
    trackedClientFields.some((field) => left[field] !== right[field]);

  const fetchWorkItems = async (filters: FilterState) => {
    const params = new URLSearchParams();

    if (filters.client) {
      params.set("client", filters.client);
    }
    if (filters.status) {
      params.set("status", filters.status);
    }
    if (filters.assignee) {
      params.set("assignee", filters.assignee);
    }
    if (filters.dueDate) {
      params.set("dueDate", filters.dueDate);
    }

    const response = await fetch(`${apiBaseUrl}/work-items?${params.toString()}`);
    const payload = (await response.json()) as WorkbenchRow[];
    const hydrated = hydrateWorkItems(payload);

    setRows(hydrated);
    setPersistedRows(hydrated);
    setSelectedRowIds([]);
    setWorkItemPage(1);
  };

  const fetchClients = async (keyword: string) => {
    const params = new URLSearchParams();

    if (keyword) {
      params.set("keyword", keyword);
    }

    const response = await fetch(`${apiBaseUrl}/clients?${params.toString()}`);
    const payload = (await response.json()) as ClientRow[];
    const hydrated = hydrateClients(payload);

    setClientRows(hydrated);
    setPersistedClientRows(hydrated);
    setSelectedClientIds([]);
  };

  const fetchAuditLogs = async (workItemId: string) => {
    const response = await fetch(`${apiBaseUrl}/work-items/${workItemId}/audit-logs`);

    if (!response.ok) {
      setAuditLogs([]);
      setAuditLogPage(1);
      return;
    }

    const payload = (await response.json()) as AuditLogRow[];
    setAuditLogs(hydrateAuditLogs(payload));
    setAuditLogPage(1);
  };

  const handleSave = async () => {
    const persistedRowMap = new Map(persistedRows.map((row) => [row.id, row]));

    let added = 0;
    let updated = 0;
    const createdRows = rows.filter((row) => !persistedRowMap.has(row.id));
    const updatedRows = rows.filter((row) => {
      const persistedRow = persistedRowMap.get(row.id);
      return persistedRow ? hasRowChanged(row, persistedRow) : false;
    });
    const deletedRows = persistedRows.filter(
      (persistedRow) => !rows.some((row) => row.id === persistedRow.id)
    );

    added = createdRows.length;
    updated = updatedRows.length;

    const requestInit = {
      headers: {
        "Content-Type": "application/json"
      }
    };

    const handleConflict = async (response: Response) => {
      if (response.status !== 409) {
        return false;
      }

      const payload = await response.json();
      window.alert(
        `충돌 발생: ${payload.entityId}\n서버 revision: ${payload.serverRevision}\n필드: ${(payload.conflictFields ?? []).join(", ")}`
      );
      return true;
    };

    for (const row of createdRows) {
      const response = await fetch(`${apiBaseUrl}/work-items`, {
        method: "POST",
        ...requestInit,
        body: JSON.stringify({
          id: row.id,
          revision: row.revision,
          client: row.client,
          bizNo: row.bizNo,
          workType: row.workType,
          status: row.status,
          assignee: row.assignee,
          dueDate: row.dueDate,
          changedBy: "insu"
        })
      });
      if (await handleConflict(response)) {
        return;
      }
    }

    for (const row of updatedRows) {
      const response = await fetch(`${apiBaseUrl}/work-items/${row.id}`, {
        method: "PATCH",
        ...requestInit,
        body: JSON.stringify({
          id: row.id,
          revision: row.revision,
          client: row.client,
          bizNo: row.bizNo,
          workType: row.workType,
          status: row.status,
          assignee: row.assignee,
          dueDate: row.dueDate,
          changedBy: "insu"
        })
      });
      if (await handleConflict(response)) {
        return;
      }
    }

    for (const row of deletedRows) {
      const response = await fetch(
        `${apiBaseUrl}/work-items/${row.id}?revision=${row.revision}&changedBy=insu`,
        { method: "DELETE" }
      );
      if (await handleConflict(response)) {
        return;
      }
    }

    await fetchWorkItems(appliedFilters);
    setSaveSummary({ added, updated, deleted: deletedRows.length });
    setToast({ message: "저장되었습니다.", visible: true });
  };

  const handleClientSave = async () => {
    const persistedRowMap = new Map(persistedClientRows.map((row) => [row.id, row]));
    let added = 0;
    let updated = 0;
    clientRows.forEach((row) => {
      const persistedRow = persistedRowMap.get(row.id);
      if (!persistedRow) {
        added += 1;
      } else if (hasClientRowChanged(row, persistedRow)) {
        updated += 1;
      }
    });

    const deleted = persistedClientRows.filter(
      (persistedRow) => !clientRows.some((row) => row.id === persistedRow.id)
    ).length;

    const response = await fetch(`${apiBaseUrl}/clients`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(
        clientRows.map((row) => ({
          id: row.id,
          name: row.name,
          bizNo: row.bizNo,
          type: row.type,
          status: row.status,
          tier: row.tier
        }))
      )
    });
    const payload = (await response.json()) as ClientRow[];
    const savedRows = hydrateClients(payload);

    setClientRows(savedRows);
    setPersistedClientRows(savedRows);
    setSelectedClientIds((current) =>
      current.filter((rowId) => savedRows.some((row) => row.id === rowId))
    );
    setClientSaveSummary({ added, updated, deleted });
    setToast({ message: "저장되었습니다.", visible: true });
  };

  const handleActionClick = (action: (typeof actionButtons)[number]) => {
    if (action === "find") {
      setAppliedFilters(draftFilters);
      void fetchWorkItems(draftFilters);
      return;
    }

    if (action === "add") {
      handleAddRow();
      return;
    }

    if (action === "del") {
      handleDeleteRows();
      return;
    }

    if (action === "save") {
      void handleSave();
      return;
    }

    if (action === "Export") {
      const headers = ["업체명", "사업자번호", "업무유형", "상태", "담당자", "마감일", "최근수정"];
      const csvRows = sortedRows.map((row) =>
        [row.client, row.bizNo, row.workType, row.status, row.assignee, row.dueDate, row.updatedAt]
          .map((value) => `"${String(value).replaceAll('"', '""')}"`)
          .join(",")
      );

      const csvContent = [headers.join(","), ...csvRows].join("\n");
      const blob = new Blob([`\uFEFF${csvContent}`], { type: "text/csv;charset=utf-8;" });
      const downloadUrl = URL.createObjectURL(blob);
      const link = document.createElement("a");

      link.href = downloadUrl;
      link.download = "tax-workbench-export.csv";
      link.click();
      URL.revokeObjectURL(downloadUrl);
      return;
    }
  };

  const handleClientActionClick = (action: Exclude<(typeof actionButtons)[number], "Export">) => {
    if (action === "find") {
      setClientAppliedFilters(clientDraftFilters);
      void fetchClients(clientDraftFilters.keyword);
      return;
    }

    if (action === "add") {
      handleAddClientRow();
      return;
    }

    if (action === "del") {
      handleDeleteClientRows();
      return;
    }

    if (action === "save") {
      void handleClientSave();
    }
  };

  const sortIndicator = (columnKey: SortKey) => {
    if (sortKey !== columnKey) {
      return "⇅";
    }

    return sortDirection === "asc" ? "↑" : "↓";
  };

  const clientSortIndicator = (columnKey: ClientEditableColumnKey) => {
    if (clientSortKey !== columnKey) {
      return "⇅";
    }

    return clientSortDirection === "asc" ? "↑" : "↓";
  };

  const handleClientSelect = () => {
    const selectedClient = clientRows.find((row) => row.id === selectedClientIds[0]);

    if (!selectedClient) {
      setShowClientModal(false);
      return;
    }

    if (pendingClientAssignmentRowId) {
      setRows((currentRows) =>
        currentRows.map((row) =>
          row.id === pendingClientAssignmentRowId
            ? {
                ...row,
                client: selectedClient.name,
                bizNo: selectedClient.bizNo
              }
            : row
        )
      );
      setPendingFocusRowId(pendingClientAssignmentRowId);
      setPendingClientAssignmentRowId(null);
    }

    setShowClientModal(false);
  };

  const renderWorkbenchCell = (row: WorkbenchRow, columnKey: EditableColumnKey) => {
    const isEditing = editingCell?.rowId === row.id && editingCell.columnKey === columnKey;
    const refKey = `${row.id}:${columnKey}`;
    const isReadOnlyColumn =
      columnKey === "status" ||
      columnKey === "client" ||
      columnKey === "bizNo" ||
      columnKey === "assignee";
    const isClientColumn = columnKey === "client";
    const cellAlignmentClass = isClientColumn ? "text-left" : "text-center";
    const contentAlignmentClass = isClientColumn ? "" : "justify-center";

    if (isEditing) {
      return (
        <div className={`min-h-10 w-full rounded-lg px-2 py-2 ${cellAlignmentClass}`}>
          {columnKey === "workType" ? (
            <select
              ref={(element) => {
                inputRefs.current[refKey] = element;
              }}
              value={row[columnKey]}
              onChange={(event) => handleCellChange(row.id, columnKey, event.target.value)}
              onBlur={handleCellCommit}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === "Escape") {
                  handleCellCommit();
                }
              }}
              className={`w-full rounded-md border border-amber-500 bg-white px-2 py-1.5 outline-none ${cellAlignmentClass}`}
            >
              {workTypeOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          ) : columnKey === "dueDate" ? (
            <input
              ref={(element) => {
                inputRefs.current[refKey] = element;
              }}
              type="date"
              value={row[columnKey]}
              onChange={(event) => handleCellChange(row.id, columnKey, event.target.value)}
              onBlur={handleCellCommit}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === "Escape") {
                  handleCellCommit();
                }
              }}
              className={`w-full rounded-md border border-amber-500 bg-white px-2 py-1.5 outline-none ${cellAlignmentClass}`}
            />
          ) : (
            <input
              ref={(element) => {
                inputRefs.current[refKey] = element;
              }}
              value={row[columnKey]}
              onChange={(event) => handleCellChange(row.id, columnKey, event.target.value)}
              onBlur={handleCellCommit}
              maxLength={columnKey === "assignee" ? 20 : columnKey === "bizNo" ? 12 : undefined}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === "Escape") {
                  handleCellCommit();
                }
              }}
              className={`w-full rounded-md border border-amber-500 bg-white px-2 py-1.5 outline-none ${cellAlignmentClass}`}
            />
          )}
        </div>
      );
    }

    return (
      <button
        type="button"
        onClick={() => handleCellClick(row.id, columnKey)}
        className={`min-h-10 w-full rounded-lg px-2 py-2 transition ${cellAlignmentClass} ${contentAlignmentClass} ${isReadOnlyColumn ? "cursor-default" : "hover:bg-amber-50"}`}
      >
        <span className={`${columnKey === "client" ? "font-medium" : ""} ${isReadOnlyColumn ? "text-amber-700" : ""}`}>
          {row[columnKey] || "-"}
        </span>
      </button>
    );
  };

  const renderClientCell = (row: ClientRow, columnKey: ClientEditableColumnKey) => {
    const isEditing = clientEditingCell?.rowId === row.id && clientEditingCell.columnKey === columnKey;
    const refKey = `${row.id}:${columnKey}`;
    const isSelectColumn =
      columnKey === "type" || columnKey === "status" || columnKey === "tier";
    const isNameColumn = columnKey === "name";
    const cellAlignmentClass = isNameColumn ? "text-left" : "text-center";
    const contentAlignmentClass = isNameColumn ? "" : "justify-center";
    const options =
      columnKey === "type"
        ? clientTypeOptions
        : columnKey === "status"
          ? clientStatusOptions
          : clientTierOptions;

    if (isEditing) {
      return (
        <div className={`min-h-10 w-full rounded-lg px-2 py-2 ${cellAlignmentClass}`}>
          {isSelectColumn ? (
            <select
              ref={(element) => {
                clientInputRefs.current[refKey] = element;
              }}
              value={row[columnKey]}
              onChange={(event) => handleClientCellChange(row.id, columnKey, event.target.value)}
              onBlur={handleClientCellCommit}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === "Escape") {
                  handleClientCellCommit();
                }
              }}
              className={`w-full rounded-md border border-amber-500 bg-white px-2 py-1.5 outline-none ${cellAlignmentClass}`}
            >
              {options.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          ) : (
            <input
              ref={(element) => {
                clientInputRefs.current[refKey] = element;
              }}
              value={row[columnKey]}
              onChange={(event) => handleClientCellChange(row.id, columnKey, event.target.value)}
              onBlur={handleClientCellCommit}
              maxLength={
                columnKey === "name" ? 100 : columnKey === "bizNo" ? 12 : undefined
              }
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === "Escape") {
                  handleClientCellCommit();
                }
              }}
              className={`w-full rounded-md border border-amber-500 bg-white px-2 py-1.5 outline-none ${cellAlignmentClass}`}
            />
          )}
        </div>
      );
    }

    return (
      <button
        type="button"
        onClick={() => handleClientCellClick(row.id, columnKey)}
        className={`min-h-10 w-full rounded-lg px-2 py-2 transition hover:bg-amber-50 ${cellAlignmentClass} ${contentAlignmentClass}`}
      >
        <span className={columnKey === "name" ? "font-medium" : ""}>{row[columnKey] || "-"}</span>
      </button>
    );
  };

  const workItemColumns = useMemo<ColumnDef<WorkbenchRow>[]>(
    () => [
      {
        id: "select",
        header: () => (
          <label className="flex items-center justify-center">
            <input
              type="checkbox"
              checked={areAllRowsSelected}
              onChange={handleToggleAllRows}
              className="h-4 w-4 rounded border-stone-400 accent-amber-600"
            />
          </label>
        ),
        cell: ({ row }) => (
          <label className="flex items-center justify-center">
            <input
              type="checkbox"
              checked={selectedRowIds.includes(row.original.id)}
              onChange={() => handleRowSelection(row.original.id)}
              className="h-4 w-4 rounded border-stone-300 accent-amber-600"
            />
          </label>
        ),
        size: 52
      },
      {
        accessorKey: "client",
        header: () => (
          <button type="button" onClick={() => handleSort("client")} className="flex w-full items-center justify-center gap-2 text-center transition hover:text-amber-200">
            <span>업체명</span>
            <span className="text-[10px] text-stone-300">{sortIndicator("client")}</span>
          </button>
        ),
        cell: ({ row }) => renderWorkbenchCell(row.original, "client"),
        size: 220
      },
      {
        accessorKey: "bizNo",
        header: () => (
          <button type="button" onClick={() => handleSort("bizNo")} className="flex w-full items-center justify-center gap-2 text-center transition hover:text-amber-200">
            <span>사업자번호</span>
            <span className="text-[10px] text-stone-300">{sortIndicator("bizNo")}</span>
          </button>
        ),
        cell: ({ row }) => (
          <div className="flex justify-center text-center">
            {renderWorkbenchCell(row.original, "bizNo")}
          </div>
        ),
        size: 170
      },
      {
        accessorKey: "workType",
        header: () => (
          <button type="button" onClick={() => handleSort("workType")} className="flex w-full items-center justify-center gap-2 text-center transition hover:text-amber-200">
            <span>업무유형</span>
            <span className="text-[10px] text-stone-300">{sortIndicator("workType")}</span>
          </button>
        ),
        cell: ({ row }) => (
          <div className="flex justify-center text-center">
            {renderWorkbenchCell(row.original, "workType")}
          </div>
        ),
        size: 140
      },
      {
        accessorKey: "status",
        header: () => (
          <button type="button" onClick={() => handleSort("status")} className="flex w-full items-center justify-center gap-2 text-center transition hover:text-amber-200">
            <span>상태</span>
            <span className="text-[10px] text-stone-300">{sortIndicator("status")}</span>
          </button>
        ),
        cell: ({ row }) => (
          <div className="flex justify-center text-center">
            {renderWorkbenchCell(row.original, "status")}
          </div>
        ),
        size: 130
      },
      {
        accessorKey: "assignee",
        header: () => (
          <button type="button" onClick={() => handleSort("assignee")} className="flex w-full items-center justify-center gap-2 text-center transition hover:text-amber-200">
            <span>담당자</span>
            <span className="text-[10px] text-stone-300">{sortIndicator("assignee")}</span>
          </button>
        ),
        cell: ({ row }) => (
          <div className="flex justify-center text-center">
            {renderWorkbenchCell(row.original, "assignee")}
          </div>
        ),
        size: 120
      },
      {
        accessorKey: "dueDate",
        header: () => (
          <button type="button" onClick={() => handleSort("dueDate")} className="flex w-full items-center justify-center gap-2 text-center transition hover:text-amber-200">
            <span>마감일</span>
            <span className="text-[10px] text-stone-300">{sortIndicator("dueDate")}</span>
          </button>
        ),
        cell: ({ row }) => (
          <div className="flex justify-center text-center">
            {renderWorkbenchCell(row.original, "dueDate")}
          </div>
        ),
        size: 140
      },
      {
        accessorKey: "updatedAt",
        header: () => (
          <button type="button" onClick={() => handleSort("updatedAt")} className="flex w-full items-center justify-center gap-2 text-center transition hover:text-amber-200">
            <span>최근수정</span>
            <span className="text-[10px] text-stone-300">{sortIndicator("updatedAt")}</span>
          </button>
        ),
        cell: ({ row }) => (
          <div className="flex justify-center text-center">
            {renderWorkbenchCell(row.original, "updatedAt")}
          </div>
        ),
        size: 110
      }
    ],
    [areAllRowsSelected, selectedRowIds, editingCell, sortKey, sortDirection]
  );

  const clientTableColumns = useMemo<ColumnDef<ClientRow>[]>(
    () => [
      {
        id: "select",
        header: () => (
          <label className="flex items-center justify-center">
            <input
              type="checkbox"
              checked={areAllClientsSelected}
              onChange={handleToggleAllClients}
              className="h-4 w-4 rounded border-stone-400 accent-amber-600"
            />
          </label>
        ),
        cell: ({ row }) => (
          <label className="flex items-center justify-center">
            <input
              type="checkbox"
              checked={selectedClientIds.includes(row.original.id)}
              onChange={() => handleClientRowSelection(row.original.id)}
              className="h-4 w-4 rounded border-stone-300 accent-amber-600"
            />
          </label>
        ),
        size: 52
      },
      {
        accessorKey: "name",
        header: () => (
          <button type="button" onClick={() => handleClientSort("name")} className="flex w-full items-center justify-center gap-2 text-center transition hover:text-amber-200">
            <span>업체명</span>
            <span className="text-[10px] text-stone-300">{clientSortIndicator("name")}</span>
          </button>
        ),
        cell: ({ row }) => renderClientCell(row.original, "name"),
        size: 280
      },
      {
        accessorKey: "bizNo",
        header: () => (
          <button type="button" onClick={() => handleClientSort("bizNo")} className="flex w-full items-center justify-center gap-2 text-center transition hover:text-amber-200">
            <span>사업자번호</span>
            <span className="text-[10px] text-stone-300">{clientSortIndicator("bizNo")}</span>
          </button>
        ),
        cell: ({ row }) => (
          <div className="flex justify-center text-center">
            {renderClientCell(row.original, "bizNo")}
          </div>
        ),
        size: 190
      },
      {
        accessorKey: "type",
        header: () => (
          <button type="button" onClick={() => handleClientSort("type")} className="flex w-full items-center justify-center gap-2 text-center transition hover:text-amber-200">
            <span>유형</span>
            <span className="text-[10px] text-stone-300">{clientSortIndicator("type")}</span>
          </button>
        ),
        cell: ({ row }) => (
          <div className="flex justify-center text-center">
            {renderClientCell(row.original, "type")}
          </div>
        ),
        size: 110
      },
      {
        accessorKey: "status",
        header: () => (
          <button type="button" onClick={() => handleClientSort("status")} className="flex w-full items-center justify-center gap-2 text-center transition hover:text-amber-200">
            <span>상태</span>
            <span className="text-[10px] text-stone-300">{clientSortIndicator("status")}</span>
          </button>
        ),
        cell: ({ row }) => (
          <div className="flex justify-center text-center">
            {renderClientCell(row.original, "status")}
          </div>
        ),
        size: 110
      },
      {
        accessorKey: "tier",
        header: () => (
          <button type="button" onClick={() => handleClientSort("tier")} className="flex w-full items-center justify-center gap-2 text-center transition hover:text-amber-200">
            <span>등급</span>
            <span className="text-[10px] text-stone-300">{clientSortIndicator("tier")}</span>
          </button>
        ),
        cell: ({ row }) => (
          <div className="flex justify-center text-center">
            {renderClientCell(row.original, "tier")}
          </div>
        ),
        size: 110
      },
      {
        accessorKey: "updatedAt",
        header: () => (
          <button type="button" onClick={() => handleClientSort("updatedAt")} className="flex w-full items-center justify-center gap-2 text-center transition hover:text-amber-200">
            <span>최근수정</span>
            <span className="text-[10px] text-stone-300">{clientSortIndicator("updatedAt")}</span>
          </button>
        ),
        cell: ({ row }) => (
          <div className="flex justify-center text-center">
            {renderClientCell(row.original, "updatedAt")}
          </div>
        ),
        size: 150
      }
    ],
    [areAllClientsSelected, selectedClientIds, clientEditingCell, clientSortKey, clientSortDirection]
  );

  const workItemTable = useReactTable({
    data: pagedWorkItemRows,
    columns: workItemColumns,
    getCoreRowModel: getCoreRowModel()
  });

  const clientTable = useReactTable({
    data: sortedClientRows,
    columns: clientTableColumns,
    getCoreRowModel: getCoreRowModel()
  });

  const auditLogColumns = useMemo<ColumnDef<AuditLogRow>[]>(
    () => [
      {
        accessorKey: "fieldName",
        header: "변경컬럼",
        cell: ({ row }) => (
          <span className="block text-center font-medium text-stone-800">
            {auditFieldLabels[row.original.fieldName] ?? row.original.fieldName ?? "-"}
          </span>
        ),
        size: 130
      },
      {
        accessorKey: "beforeValue",
        header: "이전값",
        cell: ({ row }) => (
          <span className="block text-center text-stone-600">{row.original.beforeValue || "-"}</span>
        ),
        size: 170
      },
      {
        accessorKey: "afterValue",
        header: "이후값",
        cell: ({ row }) => (
          <span className="block text-center text-stone-900">{row.original.afterValue || "-"}</span>
        ),
        size: 170
      },
      {
        accessorKey: "changedAt",
        header: "변경시각",
        cell: ({ row }) => (
          <span className="block whitespace-nowrap text-center text-stone-600">
            {row.original.changedAt || "-"}
          </span>
        ),
        size: 170
      }
    ],
    []
  );

  const auditLogTable = useReactTable({
    data: pagedAuditLogs,
    columns: auditLogColumns,
    getCoreRowModel: getCoreRowModel()
  });

  const workItemTotalPages = Math.max(1, Math.ceil(sortedRows.length / WORK_ITEM_PAGE_SIZE));
  const auditLogTotalPages = Math.max(1, Math.ceil(auditLogs.length / AUDIT_LOG_PAGE_SIZE));

  return (
    <main className="min-h-screen px-4 py-6 text-stone-900 md:px-8">
      <section className="mx-auto flex w-full max-w-[1840px] flex-col gap-4">
        <header className="rounded-[28px] border border-stone-300/70 bg-white/75 p-6 shadow-[0_18px_50px_rgba(120,94,58,0.12)] backdrop-blur">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-amber-700">
                Tax Season Workbench
              </p>
              <h1 className="mt-2 text-3xl font-semibold tracking-tight">
                Tax Workbench
              </h1>
              <p className="mt-2 max-w-3xl text-sm text-stone-600">
                Excel-like grid, inline editing, conflict-aware saves, and audit visibility
                start from this shell.
              </p>
            </div>
            <div className="grid gap-2 rounded-2xl bg-stone-900 px-4 py-3 text-sm text-stone-50">
              <span>Backend: Spring Boot 4 / Java 21</span>
              <span>Frontend: React 19 / Tailwind 4</span>
            </div>
          </div>
        </header>

        <section className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_520px] 2xl:grid-cols-[minmax(0,1fr)_560px]">
          <div className="rounded-[28px] border border-stone-300/70 bg-white/80 p-5 shadow-[0_16px_40px_rgba(98,76,48,0.08)] backdrop-blur">
            <div className="flex flex-col gap-4 border-b border-stone-200 pb-4 lg:flex-row lg:items-center lg:justify-between">
              <div className="flex flex-wrap gap-2">
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setShowClientInput((current) => !current)}
                    className="rounded-full border border-stone-300 bg-stone-50 px-4 py-2 text-sm font-medium text-stone-700 transition hover:border-amber-700 hover:text-amber-800"
                  >
                    업체명
                  </button>
                  {showClientInput ? (
                    <input
                      value={draftFilters.client}
                      onChange={(event) =>
                        setDraftFilters((current) => ({ ...current, client: event.target.value }))
                      }
                      placeholder="업체명 입력"
                      className="w-40 rounded-xl border border-stone-300 bg-white px-3 py-2 text-sm text-stone-700 outline-none"
                    />
                  ) : null}
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setShowStatusDropdown((current) => !current)}
                    className="rounded-full border border-stone-300 bg-stone-50 px-4 py-2 text-sm font-medium text-stone-700 transition hover:border-amber-700 hover:text-amber-800"
                  >
                    상태
                  </button>
                  {showStatusDropdown ? (
                    <div className="w-44 rounded-2xl border border-stone-200 bg-white p-2 shadow-lg">
                      <select
                        value={draftFilters.status}
                        onChange={(event) =>
                          setDraftFilters((current) => ({ ...current, status: event.target.value }))
                        }
                        className="w-full rounded-xl border border-stone-300 bg-white px-3 py-2 text-sm text-stone-700 outline-none"
                      >
                        <option value="">전체 상태</option>
                        {statusOptions.map((option) => (
                          <option key={option} value={option}>
                            {option}
                          </option>
                        ))}
                      </select>
                    </div>
                  ) : null}
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setShowAssigneeInput((current) => !current)}
                    className="rounded-full border border-stone-300 bg-stone-50 px-4 py-2 text-sm font-medium text-stone-700 transition hover:border-amber-700 hover:text-amber-800"
                  >
                    담당자
                  </button>
                  {showAssigneeInput ? (
                    <input
                      value={draftFilters.assignee}
                      onChange={(event) =>
                        setDraftFilters((current) => ({ ...current, assignee: event.target.value }))
                      }
                      placeholder="담당자 입력"
                      className="w-36 rounded-xl border border-stone-300 bg-white px-3 py-2 text-sm text-stone-700 outline-none"
                    />
                  ) : null}
                </div>
                <div className="flex items-center gap-2">
                  {draftFilters.dueDate ? (
                    <input
                      value={draftFilters.dueDate}
                      readOnly
                      className="w-36 rounded-xl border border-stone-300 bg-white px-3 py-2 text-sm text-stone-700 outline-none"
                    />
                  ) : null}
                  <button
                    type="button"
                    onClick={() => setShowDueDateCalendar((current) => !current)}
                    className="rounded-full border border-stone-300 bg-stone-50 px-4 py-2 text-sm font-medium text-stone-700 transition hover:border-amber-700 hover:text-amber-800"
                  >
                    마감일
                  </button>
                  {showDueDateCalendar ? (
                    <div className="rounded-2xl border border-stone-200 bg-white p-3 shadow-lg">
                      <input
                        type="date"
                        value={draftFilters.dueDate}
                        onChange={(event) => {
                          setDraftFilters((current) => ({ ...current, dueDate: event.target.value }));
                          setShowDueDateCalendar(false);
                        }}
                        className="rounded-xl border border-stone-300 bg-white px-3 py-2 text-sm text-stone-700 outline-none"
                      />
                    </div>
                  ) : null}
                </div>
              </div>
              <div className="flex flex-wrap justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowClientModal(true)}
                  className="rounded-xl border border-stone-300 bg-stone-900 px-4 py-2 text-sm font-semibold text-stone-50 shadow-sm transition hover:border-amber-400 hover:text-amber-200"
                >
                  법인정보
                </button>
                {actionButtons.map((label) => (
                  <button
                    key={label}
                    type="button"
                    onClick={() => handleActionClick(label)}
                    className="rounded-xl border border-stone-300 bg-white px-4 py-2 text-sm font-semibold text-stone-800 shadow-sm transition hover:border-amber-700 hover:text-amber-800"
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            {saveSummary ? (
              <div className="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
                저장 완료: 추가 {saveSummary.added}건, 수정 {saveSummary.updated}건, 삭제 {saveSummary.deleted}건
              </div>
            ) : null}

            <div className="mt-4 overflow-hidden rounded-2xl border border-stone-200">
              <div className="overflow-x-auto">
                <table className="min-w-full table-fixed border-collapse">
                  <thead className="bg-stone-900 text-xs font-semibold tracking-[0.08em] text-stone-50">
                    {workItemTable.getHeaderGroups().map((headerGroup) => (
                      <tr key={headerGroup.id}>
                        {headerGroup.headers.map((header) => (
                          <th key={header.id} className="px-4 py-3 text-center" style={{ width: header.getSize() }}>
                            {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
                          </th>
                        ))}
                      </tr>
                    ))}
                  </thead>
                  <tbody className="divide-y divide-stone-200 text-sm text-stone-900">
                    {workItemTable.getRowModel().rows.map((row, index) => (
                      <tr
                        key={row.id}
                        ref={(element) => {
                          workItemRowRefs.current[row.original.id] = element;
                        }}
                        tabIndex={0}
                        onClick={() => setActiveWorkItemId(row.original.id)}
                        className={`cursor-pointer outline-none ${activeWorkItemId === row.original.id ? "bg-amber-50 ring-1 ring-inset ring-amber-300" : index % 2 === 1 ? "bg-stone-50" : "bg-white"} focus-visible:ring-2 focus-visible:ring-amber-400`}
                      >
                        {row.getVisibleCells().map((cell) => (
                          <td key={cell.id} className="px-4 py-2 align-middle" style={{ width: cell.column.getSize() }}>
                            {flexRender(cell.column.columnDef.cell, cell.getContext())}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="flex items-center justify-between border-t border-stone-200 bg-stone-50 px-4 py-3 text-sm text-stone-600">
                <span>
                  페이지 {workItemPage} / {workItemTotalPages}
                </span>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setWorkItemPage((current) => Math.max(1, current - 1))}
                    disabled={workItemPage === 1}
                    className="rounded-lg border border-stone-300 bg-white px-3 py-1.5 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    이전
                  </button>
                  <button
                    type="button"
                    onClick={() =>
                      setWorkItemPage((current) => Math.min(workItemTotalPages, current + 1))
                    }
                    disabled={workItemPage === workItemTotalPages}
                    className="rounded-lg border border-stone-300 bg-white px-3 py-1.5 disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    다음
                  </button>
                </div>
              </div>
            </div>
          </div>
          <aside className="rounded-[28px] border border-stone-300/70 bg-white/80 p-5 shadow-[0_16px_40px_rgba(98,76,48,0.08)] backdrop-blur">
            <div className="border-b border-stone-200 pb-4">
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-amber-700">
                Audit Trail
              </p>
              <h2 className="mt-2 text-2xl font-semibold text-stone-900">변경로그</h2>
              <p className="mt-1 text-sm text-stone-600">
                선택한 행의 변경 컬럼, 이전값, 이후값을 표시합니다.
              </p>
              {activeWorkItemId ? (
                <p className="mt-2 text-xs font-medium text-stone-500">
                  선택된 WorkItem ID: {activeWorkItemId}
                </p>
              ) : null}
            </div>

            <div className="mt-4 overflow-hidden rounded-2xl border border-stone-200">
              {activeWorkItemId ? (
                auditLogs.length > 0 ? (
                  <div>
                    <div className="overflow-x-auto">
                      <table className="min-w-full table-fixed border-collapse">
                        <thead className="bg-stone-900 text-xs font-semibold tracking-[0.08em] text-stone-50">
                          {auditLogTable.getHeaderGroups().map((headerGroup) => (
                            <tr key={headerGroup.id}>
                              {headerGroup.headers.map((header) => (
                                <th key={header.id} className="px-4 py-3 text-center" style={{ width: header.getSize() }}>
                                  {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
                                </th>
                              ))}
                            </tr>
                          ))}
                        </thead>
                        <tbody className="divide-y divide-stone-200 text-sm text-stone-900">
                          {auditLogTable.getRowModel().rows.map((row, index) => (
                            <tr key={row.id} className={index % 2 === 1 ? "bg-stone-50" : "bg-white"}>
                              {row.getVisibleCells().map((cell) => (
                                <td key={cell.id} className="px-4 py-3 align-top" style={{ width: cell.column.getSize() }}>
                                  {flexRender(cell.column.columnDef.cell, cell.getContext())}
                                </td>
                              ))}
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                    <div className="flex items-center justify-between border-t border-stone-200 bg-stone-50 px-4 py-3 text-sm text-stone-600">
                      <span>
                        페이지 {auditLogPage} / {auditLogTotalPages}
                      </span>
                      <div className="flex gap-2">
                        <button
                          type="button"
                          onClick={() => setAuditLogPage((current) => Math.max(1, current - 1))}
                          disabled={auditLogPage === 1}
                          className="rounded-lg border border-stone-300 bg-white px-3 py-1.5 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                          이전
                        </button>
                        <button
                          type="button"
                          onClick={() =>
                            setAuditLogPage((current) => Math.min(auditLogTotalPages, current + 1))
                          }
                          disabled={auditLogPage === auditLogTotalPages}
                          className="rounded-lg border border-stone-300 bg-white px-3 py-1.5 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                          다음
                        </button>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="px-4 py-10 text-sm text-stone-500">변경로그가 없습니다.</div>
                )
              ) : (
                <div className="px-4 py-10 text-sm text-stone-500">
                  행을 선택하면 변경로그가 표시됩니다.
                </div>
              )}
            </div>
          </aside>
        </section>
      </section>

      {showClientModal ? (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-stone-950/45 px-4 py-8">
          <div className="flex max-h-[85vh] w-full max-w-6xl flex-col overflow-hidden rounded-[32px] border border-stone-300 bg-white shadow-[0_28px_80px_rgba(28,25,23,0.28)]">
            <div className="border-b border-stone-200 px-6 py-5">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-amber-700">
                    Client Model
                  </p>
                  <h2 className="mt-2 text-2xl font-semibold text-stone-900">법인정보</h2>
                  <p className="mt-1 text-sm text-stone-600">
                    Client 정보를 조회하고 추가, 삭제, 저장할 수 있습니다.
                  </p>
                </div>
              </div>
            </div>

            <div className="flex flex-1 flex-col overflow-hidden px-6 py-5">
              <div className="flex flex-col gap-4 border-b border-stone-200 pb-4 lg:flex-row lg:items-center lg:justify-between">
                <div className="flex items-center gap-3">
                  <input
                    value={clientDraftFilters.keyword}
                    onChange={(event) =>
                      setClientDraftFilters({ keyword: event.target.value })
                    }
                    placeholder="업체명 또는 사업자번호"
                    className="w-64 rounded-xl border border-stone-300 bg-white px-3 py-2 text-sm text-stone-700 outline-none"
                  />
                </div>
                <div className="flex flex-wrap justify-end gap-2">
                  {actionButtons
                    .filter((label) => label !== "Export")
                    .map((label) => (
                      <button
                        key={label}
                        type="button"
                        onClick={() => handleClientActionClick(label)}
                        className="rounded-xl border border-stone-300 bg-white px-4 py-2 text-sm font-semibold text-stone-800 shadow-sm transition hover:border-amber-700 hover:text-amber-800"
                      >
                        {label}
                      </button>
                    ))}
                </div>
              </div>

              {clientSaveSummary ? (
                <div className="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
                  저장 완료: 추가 {clientSaveSummary.added}건, 수정 {clientSaveSummary.updated}건, 삭제 {clientSaveSummary.deleted}건
                </div>
              ) : null}

              <div className="mt-4 overflow-hidden rounded-2xl border border-stone-200">
                <div className="overflow-x-auto border-b border-stone-200">
                  <table className="min-w-full table-fixed border-collapse">
                    <thead className="bg-stone-900 text-xs font-semibold tracking-[0.08em] text-stone-50">
                      {clientTable.getHeaderGroups().map((headerGroup) => (
                        <tr key={headerGroup.id}>
                          {headerGroup.headers.map((header) => (
                            <th key={header.id} className="px-4 py-3 text-center" style={{ width: header.getSize() }}>
                              {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
                            </th>
                          ))}
                        </tr>
                      ))}
                    </thead>
                  </table>
                </div>
                <div className="max-h-[265px] overflow-y-auto overflow-x-auto">
                  <table className="min-w-full table-fixed border-collapse">
                    <tbody className="divide-y divide-stone-200 text-sm text-stone-900">
                      {clientTable.getRowModel().rows.map((row, index) => (
                        <tr key={row.id} className={index % 2 === 1 ? "bg-stone-50" : "bg-white"}>
                          {row.getVisibleCells().map((cell) => (
                            <td key={cell.id} className="px-4 py-2 align-middle" style={{ width: cell.column.getSize() }}>
                              {flexRender(cell.column.columnDef.cell, cell.getContext())}
                            </td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              <div className="mt-5 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={handleClientSelect}
                  className="rounded-xl border border-amber-700 bg-amber-700 px-4 py-2 text-sm font-semibold text-white transition hover:bg-amber-800"
                >
                  select
                </button>
                <button
                  type="button"
                  onClick={() => setShowClientModal(false)}
                  className="rounded-xl border border-stone-300 bg-white px-4 py-2 text-sm font-semibold text-stone-800 transition hover:border-stone-500"
                >
                  close
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {toast ? (
        <div className="pointer-events-none fixed bottom-6 left-1/2 z-50 -translate-x-1/2">
          <div
            className={`rounded-full bg-stone-900 px-5 py-3 text-sm font-medium text-white shadow-[0_12px_30px_rgba(28,25,23,0.28)] transition-all duration-300 ${
              toast.visible ? "translate-y-0 opacity-100" : "translate-y-3 opacity-0"
            }`}
          >
            {toast.message}
          </div>
        </div>
      ) : null}
    </main>
  );
}
