# Frontend Run Guide

## Prerequisites

- Node.js 20+
- npm 10+

## Run

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server starts on `http://localhost:5173`.

## Build

```bash
npm run build
```

## Notes

- `/api` requests are proxied to `http://localhost:8080`
- the workbench expects the Spring backend to be running locally
- bulk import form accepts the backend bulk-import JSON contract directly
- export button opens the backend CSV export endpoint for the current filter slice
