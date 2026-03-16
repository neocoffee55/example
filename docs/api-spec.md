# Tax Workbench API 명세

이 문서는 현재 저장소에 구현되어 있는 백엔드 API를 기준으로 작성한 명세입니다.

범위:

- 로컬 개발용 API
- WorkItem 생성/수정/삭제/조회
- WorkItem 변경로그 조회
- Client 조회 및 저장
- optimistic locking 충돌 응답

기본 실행 기준:

- Backend Base URL: `http://localhost:8081`
- Frontend Dev Server: `http://localhost:8080`
- 프론트는 `/api/...`로 호출하고 Vite가 백엔드로 프록시합니다.

## 공통 규칙

- Content-Type: `application/json`
- 날짜 형식
  - `dueDate`: `YYYY-MM-DD`
  - `updatedAt`, `changedAt`: 백엔드 ISO timestamp 문자열
- CORS
  - 현재 백엔드는 `http://localhost:8080`을 허용합니다.

## 1. WorkItem API

### 1.1 WorkItem 조회

`GET /api/work-items`

Query Parameter:

- `client`: 업체명 부분일치
- `status`: 상태 정확일치
- `assignee`: 담당자 부분일치
- `dueDate`: `YYYY-MM-DD` 정확일치

예시:

```http
GET /api/work-items?client=Han&status=TODO&dueDate=2026-03-20
```

응답 `200 OK`:

```json
[
  {
    "id": "WI-10031",
    "revision": 1,
    "client": "Han River Holdings",
    "bizNo": "123-45-67890",
    "workType": "FILING",
    "status": "TODO",
    "assignee": "insu",
    "dueDate": "2026-03-20",
    "updatedAt": "2026-03-15T01:20:33.512Z"
  }
]
```

참고:

- 현재 백엔드 기본 정렬은 `clientName ASC`, `workType ASC`입니다.
- 필터링은 현재 서비스 레이어에서 repository 조회 후 적용합니다.

### 1.2 WorkItem 생성

`POST /api/work-items`

요청 본문:

```json
{
  "id": "WI-1710000000000",
  "revision": 0,
  "client": "Han River Holdings",
  "bizNo": "123-45-67890",
  "workType": "FILING",
  "status": "TODO",
  "assignee": "insu",
  "dueDate": "2026-03-20",
  "changedBy": "insu"
}
```

응답 `200 OK`:

```json
{
  "id": "WI-1710000000000",
  "revision": 0,
  "client": "Han River Holdings",
  "bizNo": "123-45-67890",
  "workType": "FILING",
  "status": "TODO",
  "assignee": "insu",
  "dueDate": "2026-03-20",
  "updatedAt": "2026-03-15T01:22:00.210Z"
}
```

참고:

- 신규 WorkItem 생성 시에는 현재 audit log가 기록되지 않습니다.

### 1.3 WorkItem 수정

`PATCH /api/work-items/{id}`

Path Parameter:

- `id`: WorkItem id

요청 본문:

```json
{
  "id": "WI-10031",
  "revision": 1,
  "client": "Han River Holdings",
  "bizNo": "123-45-67890",
  "workType": "REVIEW",
  "status": "TODO",
  "assignee": "insu",
  "dueDate": "2026-03-21",
  "changedBy": "insu"
}
```

응답 `200 OK`:

```json
{
  "id": "WI-10031",
  "revision": 2,
  "client": "Han River Holdings",
  "bizNo": "123-45-67890",
  "workType": "REVIEW",
  "status": "TODO",
  "assignee": "insu",
  "dueDate": "2026-03-21",
  "updatedAt": "2026-03-15T01:24:11.892Z"
}
```

참고:

- `revision`을 기준으로 optimistic locking을 수행합니다.
- 수정 성공 시 변경된 필드에 대해 audit log가 기록됩니다.

### 1.4 WorkItem 삭제

`DELETE /api/work-items/{id}?revision=...&changedBy=...`

Path Parameter:

- `id`: WorkItem id

Query Parameter:

- `revision`: 클라이언트가 알고 있는 현재 revision
- `changedBy`: 선택값, 기본값은 `insu`

예시:

```http
DELETE /api/work-items/WI-10031?revision=2&changedBy=insu
```

응답:

- `200 OK`
- 빈 본문

참고:

- 삭제도 optimistic locking revision 검사를 수행합니다.
- 삭제 시에는 `beforeValue`만 채우고 `afterValue`는 빈 값으로 audit log를 기록합니다.

### 1.5 WorkItem 변경로그 조회

`GET /api/work-items/{id}/audit-logs`

Path Parameter:

- `id`: WorkItem id

응답 `200 OK`:

```json
[
  {
    "workItemId": "WI-10031",
    "revision": 2,
    "changedAt": "2026-03-15T01:24:11.902Z",
    "changedBy": "insu",
    "fieldName": "workType",
    "beforeValue": "FILING",
    "afterValue": "REVIEW"
  },
  {
    "workItemId": "WI-10031",
    "revision": 2,
    "changedAt": "2026-03-15T01:24:11.902Z",
    "changedBy": "insu",
    "fieldName": "dueDate",
    "beforeValue": "2026-03-20",
    "afterValue": "2026-03-21"
  }
]
```

참고:

- `changedAt DESC` 기준 최신순으로 반환됩니다.
- `fieldName`은 백엔드 필드 키 그대로 내려가고, 프론트에서 한글 헤더명으로 매핑합니다.

## 2. Client API

### 2.1 Client 조회

`GET /api/clients`

Query Parameter:

- `keyword`: 업체명 또는 사업자번호 부분일치

예시:

```http
GET /api/clients?keyword=123-45
```

응답 `200 OK`:

```json
[
  {
    "id": "CL-1001",
    "name": "Han River Holdings",
    "bizNo": "123-45-67890",
    "type": "CORPORATE",
    "status": "ACTIVE",
    "tier": "VIP",
    "updatedAt": "2026-03-15T01:10:00.001Z"
  }
]
```

### 2.2 Client 저장

`PUT /api/clients`

요청 본문:

```json
[
  {
    "id": "CL-1001",
    "name": "Han River Holdings",
    "bizNo": "123-45-67890",
    "type": "CORPORATE",
    "status": "ACTIVE",
    "tier": "VIP"
  },
  {
    "id": "CL-1710000000000",
    "name": "New Client",
    "bizNo": "111-22-33333",
    "type": "CORPORATE",
    "status": "ACTIVE",
    "tier": "BASIC"
  }
]
```

응답 `200 OK`:

```json
[
  {
    "id": "CL-1001",
    "name": "Han River Holdings",
    "bizNo": "123-45-67890",
    "type": "CORPORATE",
    "status": "ACTIVE",
    "tier": "VIP",
    "updatedAt": "2026-03-15T01:30:10.112Z"
  },
  {
    "id": "CL-1710000000000",
    "name": "New Client",
    "bizNo": "111-22-33333",
    "type": "CORPORATE",
    "status": "ACTIVE",
    "tier": "BASIC",
    "updatedAt": "2026-03-15T01:30:10.112Z"
  }
]
```

참고:

- 현재 Client 저장은 row 단위가 아니라 전체 교체형 sync 모델입니다.
- 백엔드는 기존 Client 데이터를 batch delete 후, 요청 본문 전체를 다시 저장합니다.

## 3. 충돌 응답

WorkItem 수정 또는 삭제 시 오래된 `revision`으로 요청하면 백엔드는 `409 Conflict`를 반환합니다.

응답 형식:

```json
{
  "entityId": "WI-10031",
  "serverRevision": 3,
  "conflictFields": ["workType", "dueDate"],
  "serverSnapshot": {
    "client": "Han River Holdings",
    "bizNo": "123-45-67890",
    "workType": "BOOKKEEPING",
    "status": "TODO",
    "assignee": "insu",
    "dueDate": "2026-03-22"
  },
  "attemptedChanges": {
    "client": "Han River Holdings",
    "bizNo": "123-45-67890",
    "workType": "REVIEW",
    "status": "TODO",
    "assignee": "insu",
    "dueDate": "2026-03-21"
  },
  "message": "Work item was modified by another request."
}
```

의미:

- `entityId`: 충돌이 발생한 WorkItem id
- `serverRevision`: 서버의 최신 revision
- `conflictFields`: 서버 값과 요청 값이 다른 필드 목록
- `serverSnapshot`: 서버 기준 최신 값
- `attemptedChanges`: 클라이언트가 보낸 값
- `message`: 충돌 메시지

## 4. 필드 기준

### WorkItem 필드

- `id`: string
- `revision`: number
- `client`: string
- `bizNo`: string
- `workType`: `FILING | BOOKKEEPING | REVIEW | ETC`
- `status`: `TODO | IN_PROGRESS | DONE | HOLD`
- `assignee`: string
- `dueDate`: `YYYY-MM-DD`
- `updatedAt`: timestamp string

### Client 필드

- `id`: string
- `name`: string
- `bizNo`: string
- `type`: `CORPORATE | INDIVIDUAL`
- `status`: `ACTIVE | INACTIVE`
- `tier`: `BASIC | PREMIUM | VIP`
- `updatedAt`: timestamp string

### WorkItemAudit 필드

- `workItemId`: string
- `revision`: number
- `changedAt`: timestamp string
- `changedBy`: string
- `fieldName`: string
- `beforeValue`: string
- `afterValue`: string

## 5. 현재 한계

- WorkItem 목록 조회는 아직 DB 주도 페이징이 아닙니다.
- Client 저장은 아직 row 단위 API가 아닙니다.
- 정식 OpenAPI 문서는 아직 없습니다.
- 입력 검증은 현재 UI 동작에 맞춘 수준으로 가볍습니다.
- CSV Export는 아직 백엔드 streaming이 아니라 프론트에서 현재 보이는 데이터를 기준으로 생성합니다.

## 6. 문서 기준 소스

이 문서는 현재 구현 기준으로 다음 파일을 기준으로 작성했습니다.

- [WorkItemController.java](/Users/insu_han/IdeaProjects/example/backend/src/main/java/com/taxworkbench/interfaces/http/WorkItemController.java)
- [ClientController.java](/Users/insu_han/IdeaProjects/example/backend/src/main/java/com/taxworkbench/interfaces/http/ClientController.java)
- [ApiExceptionHandler.java](/Users/insu_han/IdeaProjects/example/backend/src/main/java/com/taxworkbench/interfaces/http/ApiExceptionHandler.java)
- [WorkbenchDataService.java](/Users/insu_han/IdeaProjects/example/backend/src/main/java/com/taxworkbench/application/WorkbenchDataService.java)
