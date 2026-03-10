# Frontend Run Guide

## Scope

This document explains how to run the Tax Workbench frontend in local development.

## Prerequisites

- Node.js 22.12+ recommended for the current Vite toolchain
- npm 10+

Note:

- The current local machine is on Node `22.11.0`.
- Production build succeeded, but Vite prints an engine warning because it recommends Node `22.12+`.

## Location

- Project root: `/Users/insu_han/IdeaProjects/example`
- Frontend module: `/Users/insu_han/IdeaProjects/example/frontend`

## Install Dependencies

```bash
cd /Users/insu_han/IdeaProjects/example/frontend
npm install
```

## Run Development Server

```bash
cd /Users/insu_han/IdeaProjects/example/frontend
npm run dev
```

Default dev server:

- `http://localhost:5173`

## Build for Production

```bash
cd /Users/insu_han/IdeaProjects/example/frontend
npm run build
```

## Preview Production Build

```bash
cd /Users/insu_han/IdeaProjects/example/frontend
npm run preview
```

## Current Frontend Stack

- React 19
- TypeScript
- Tailwind CSS
- TanStack Query

Main entry points:

- `/Users/insu_han/IdeaProjects/example/frontend/src/main.tsx`
- `/Users/insu_han/IdeaProjects/example/frontend/src/workbench/WorkbenchShell.tsx`

## Known Notes

- The current screen is a Step 1 shell, not the full Workbench implementation.
- Virtualized grid, inline edit, conflict handling, and audit drawer belong to later steps.
