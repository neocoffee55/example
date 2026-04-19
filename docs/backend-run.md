# Backend Run Guide

## Prerequisites

- Java 21
- Maven 3.9+

## Run

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

## Notes

- H2 in-memory database is used for local execution
- sample seed data is inserted at startup if the database is empty
- H2 console is enabled at `http://localhost:8080/h2-console`
- JPA schema mode is `update`

## Main Endpoints

- `GET /api/work-items`
- `PATCH /api/work-items/{id}`
- `GET /api/work-items/{id}/audit-logs`
- `POST /api/work-items/bulk-import`
- `GET /api/work-items/export`
- `GET /api/clients`
