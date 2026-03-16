# Tax Workbench

Tax Workbench는 세무 업무 운영 환경을 염두에 둔 워크벤치입니다. 핵심 목표는 두 가지입니다. 첫째는 그리드 중심의 빠른 작업 흐름이고, 둘째는 여러 사용자가 동시에 데이터를 수정해도 정합성이 무너지지 않는 백엔드 구조입니다. 현재 코드베이스에는 H2 기반 Spring Boot 백엔드, React 기반 워크벤치 UI, 인라인 편집, 변경로그 조회, 법인정보 팝업, CSV Export, optimistic locking 충돌 처리, 그리드 페이징이 포함되어 있습니다.

## How to Run

### 사전 준비

- Java 21 대상
- Maven 3.9+
- Node.js 22.12+ 권장
- npm 10+

참고:

- 백엔드는 [backend/pom.xml](/Users/insu_han/IdeaProjects/example/backend/pom.xml)에서 `java.version=21`로 설정되어 있습니다.
- 현재 로컬 환경의 Node는 `22.11.0`이라 Vite가 권장 버전 경고를 출력할 수 있습니다.
- 로컬 실행에는 별도의 환경 변수 설정이 필요하지 않습니다. 데이터베이스는 기본적으로 in-memory H2를 사용합니다.

### Backend

백엔드 실행:

```bash
cd /Users/insu_han/IdeaProjects/example/backend
mvn spring-boot:run
```

현재 로컬 기본값:

- Base URL: `http://localhost:8081`
- H2 JDBC URL: `jdbc:h2:mem:taxworkbench;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- H2 콘솔: 활성화
- SQL 로그: 활성화

주요 설정 파일:

- [application.yml](/Users/insu_han/IdeaProjects/example/backend/src/main/resources/application.yml)

백엔드 테스트 실행:

```bash
cd /Users/insu_han/IdeaProjects/example/backend
mvn test
```

### Frontend

의존성 설치 및 개발 서버 실행:

```bash
cd /Users/insu_han/IdeaProjects/example/frontend
npm install
npm run dev
```

현재 로컬 기본값:

- Frontend URL: `http://localhost:8080`
- `/api` 프록시 대상: `http://localhost:8081`

Vite 설정 파일:

- [vite.config.ts](/Users/insu_han/IdeaProjects/example/frontend/vite.config.ts)

프론트 빌드:

```bash
cd /Users/insu_han/IdeaProjects/example/frontend
npm run build
```

### 자주 사용하는 로컬 엔드포인트

- WorkItem 조회: `GET /api/work-items`
- WorkItem 생성: `POST /api/work-items`
- WorkItem 수정: `PATCH /api/work-items/{id}`
- WorkItem 삭제: `DELETE /api/work-items/{id}?revision=...&changedBy=...`
- WorkItem 변경로그 조회: `GET /api/work-items/{id}/audit-logs`
- Client 조회: `GET /api/clients`
- Client 일괄 저장: `PUT /api/clients`
- Actuator Health: `GET http://localhost:8081/actuator/health`

실행 관련 상세 문서:

- [docs/backend-run.md](/Users/insu_han/IdeaProjects/example/docs/backend-run.md)
- [docs/frontend-run.md](/Users/insu_han/IdeaProjects/example/docs/frontend-run.md)
- [docs/api-spec.md](/Users/insu_han/IdeaProjects/example/docs/api-spec.md)

## Architectural Rationale

이 프로젝트는 백엔드의 정합성과 프론트엔드의 작업 속도를 동시에 가져가는 방향으로 구조를 잡았습니다.

### Backend 설계 의도

백엔드는 DDD 스타일의 큰 경계를 유지하고 있습니다.

- `application`: 유스케이스 orchestration, row 단위 저장 흐름, 충돌 처리
- `infrastructure.persistence`: JPA 엔티티와 리포지토리
- `interfaces.http`: 컨트롤러와 예외 응답 매핑
- `bootstrap`: 초기 데이터 적재
- `domain`: 이후 정책 분리를 위한 경계

아직 완성된 도메인 계층이라고 보기는 어렵지만, 비즈니스 규칙이 컨트롤러에 흩어지지 않도록 구조를 먼저 고정해 둔 상태입니다. 현재 [WorkbenchDataService.java](/Users/insu_han/IdeaProjects/example/backend/src/main/java/com/taxworkbench/application/WorkbenchDataService.java)가 다음 책임을 모아서 처리합니다.

- 목록 조회
- WorkItem 생성/수정/삭제
- optimistic locking 충돌 판정
- audit log 기록
- Client 저장 orchestration

이 선택의 이유는 HTTP 레이어를 얇게 유지하고, 이후 `CreateWorkItemUseCase`, `UpdateWorkItemUseCase`, `ClientPolicy` 같은 더 세밀한 구조로 나눌 수 있는 여지를 남기기 위해서입니다.

또한 `work_item`과 `work_item_audit_log`를 분리한 것은 의도적인 결정입니다. 변경로그 조회는 메인 목록과 별도의 조회 경로로 다루고 있으며, 목록 조회가 audit 데이터 증가 때문에 느려지지 않도록 경계를 나눴습니다.

### Frontend 설계 의도

프론트엔드는 [WorkbenchShell.tsx](/Users/insu_han/IdeaProjects/example/frontend/src/workbench/WorkbenchShell.tsx) 중심으로 동작합니다. 현재는 컴포넌트 분해보다 기능 검증과 작업 흐름 재현이 우선이기 때문에, 화면이 운영자 콘솔처럼 동작하는 데 초점을 맞췄습니다.

- 왼쪽 WorkItem 그리드
- 오른쪽 변경로그 그리드
- 법인정보 팝업
- 상단 필터/액션 바
- 인라인 편집

TanStack Table을 선택한 이유는 헤더, 셀, 정렬, 페이징 제어를 세밀하게 직접 다룰 수 있기 때문입니다. 현재 요구사항은 “완성형 그리드 컴포넌트 도입”보다 “업무 흐름에 맞춘 세부 제어”가 더 중요하다고 판단했습니다.

또한 프론트 동작도 백엔드의 정합성 전략을 그대로 따릅니다.

- 전체 폼 저장 대신 row 단위 저장
- 첫 행 자동 선택 및 변경로그 자동 조회
- 충돌 판정은 프론트 추정이 아니라 백엔드 응답 기준

## Trade-off Analysis

현재 설계는 몇 가지 분명한 타협을 포함합니다.

### 얻은 점

- WorkItem `POST`, `PATCH`, `DELETE`가 분리되어 트랜잭션 의미가 명확합니다.
- H2와 Spring Boot만으로 전체 흐름을 로컬에서 바로 확인할 수 있습니다.
- 변경로그를 메인 그리드 조회 경로와 분리해서 다룰 수 있습니다.
- 프론트에서 셀 편집, 팝업 흐름, 토스트, 우측 변경로그 패널 같은 동작을 빠르게 조정할 수 있습니다.

### 비용

- 백엔드의 `WorkbenchDataService`가 아직 넓은 책임을 가지고 있습니다. 장기적으로는 유스케이스와 정책 객체로 더 쪼개야 합니다.
- 법인정보 팝업은 아직 `PUT /api/clients` 기반의 sync 저장입니다. WorkItem은 row 단위인데 Client는 아직 그렇지 않아서 구조가 완전히 대칭적이지 않습니다.
- 프론트 워크벤치가 하나의 큰 파일에 집중되어 있습니다. 빠른 반복에는 유리했지만, 장기 유지보수 관점에서는 `grid`, `audit`, `client-popup`, `toast` 등으로 분리하는 게 맞습니다.
- 목록 조회와 필터는 아직 repository 조회 후 메모리에서 후처리합니다. 로컬 H2와 현재 단계에서는 허용 가능하지만, 10만 건 이상을 가정하는 운영 구조로는 부족합니다.

### 지금 이 타협이 허용되는 이유

현재 저장소는 “최종 플랫폼”보다는 “동작하는 아키텍처 프로토타입”에 가깝습니다. 지금 단계에서 중요한 것은 다음을 실제 코드로 검증하는 것입니다.

- 그리드 중심 편집
- optimistic locking 저장
- 변경로그 분리 조회
- 법인정보 팝업을 통한 Client 선택
- DB 기반 end-to-end 흐름

이 동작이 고정된 뒤에야 조회 최적화, 정책 분리, 비동기 처리 같은 다음 단계의 고도화가 의미를 가집니다.

## Cloud & PaaS Strategy

이 시스템이 실제 신고 시즌 트래픽을 받는다면, 현재 로컬 구조는 동기형 운영 API와 비동기형 대량 처리 워크로드를 분리하는 방향으로 확장되어야 합니다.

### 권장 프로덕션 구조

- Spring Boot API는 `Container Apps` 같은 컨테이너 기반 PaaS에 배치
- 주 데이터는 관리형 관계형 DB로 이전
- 대량 Export 파일은 오브젝트 스토리지에 저장
- Bulk Insert, Export, Audit 후처리는 메시지 큐 기반 비동기 작업으로 분리
- 장시간 작업은 별도 worker 앱 또는 serverless function에서 처리

### Azure 기준 확장 예시

- `Azure Container Apps`
  - Spring Boot API와 React 정적 프론트를 분리 배치
  - 동시 요청 수와 큐 적체량 기반으로 자동 확장
- `Azure Database for PostgreSQL`
  - H2 대체
  - `biz_no_snapshot`, `status`, `assignee`, `updated_at`, `due_date` 중심 인덱스 설계
  - 백업, 복구, 모니터링 체계 확보
- `Azure Service Bus`
  - Bulk Insert 작업 큐
  - Export 작업 큐
  - 필요 시 audit persistence를 비동기 경로로 분리
- `Azure Functions`
  - Export 작업을 받아 chunk 단위로 읽고 파일 생성
  - Bulk Insert를 chunk 단위 검증/저장
  - audit 아카이빙, retention 작업 실행
- `Azure Blob Storage`
  - 생성된 CSV 파일 저장
  - 대용량 Export는 동기 응답 대신 다운로드 링크 방식으로 제공
- `Azure Monitor`, `Application Insights`
  - 느린 쿼리, 충돌 빈도, 큐 적체, Export 소요 시간 추적

### 확장 전략의 핵심

1. 목록 조회

- 현재 `findAll()` 후 메모리 필터 구조를 DB 주도 필터/정렬/페이징 구조로 전환
- projection query 또는 native SQL 도입
- 고카디널리티 정렬에는 keyset pagination 검토

2. 변경로그 증가 대응

- append-only 구조 유지
- 목록 경로와 완전히 분리
- 필요 시 Service Bus 이벤트 기반 비동기 저장으로 전환
- 보관 기간 및 아카이빙 정책 도입

3. Export

- 현재는 브라우저에서 보이는 데이터를 CSV로 내보냄
- 운영 환경에서는 Export 요청 → job id 반환 → 상태 조회 → Blob Storage 다운로드 링크 제공 구조로 전환

4. Bulk Insert

- 대량 입력은 동기 API 처리 대신 job submission 방식으로 이동
- chunk 단위 실패 결과를 별도 리포트로 저장
- worker를 분리해서 운영 API latency를 보호

5. 동시성

- optimistic locking 유지
- conflict metadata를 더 풍부하게 내려서 프론트에서 비교/머지 UX 지원
- 재시도 요청에 대한 idempotency key 도입 검토

요약하면, 운영 전략의 핵심은 “현재 API 서버를 단순히 더 크게 만드는 것”이 아니라, “상호작용이 필요한 운영 API는 얇고 안정적으로 유지하고, 무겁고 대량인 작업은 큐와 워커로 분리하는 것”입니다.

## AI Collaboration Reflection

이번 구현에서 AI는 자동 코더가 아니라 빠른 구현 파트너로 사용되었습니다.

### AI가 크게 도움이 된 부분

- 백엔드와 프론트 초기 구조를 빠르게 세팅
- 반복적인 UI 수정 작업을 빠르게 반영
- 그리드, 팝업, 토스트, 변경로그 패널 같은 상호작용을 빠르게 조정
- 현재 구조를 설명하는 문서 초안을 빠르게 정리

### AI가 그대로 믿기 어려웠던 부분

- UI 수정이 반복되면서 포커스, 페이징, 팝업 입력 같은 부분에서 회귀가 발생함
- 문서가 실제 런타임 설정보다 오래된 상태를 유지하는 경우가 있었음
- 설계 설명이 실제 구현보다 앞서 나가서, 문서와 코드가 어긋나는 순간이 있었음

### 한계를 어떻게 보완했는가

- `mvn test`, `npm run build`로 반복 검증
- 포트, 프록시, 저장 흐름 같은 런타임 정보는 실제 코드 기준으로 재확인
- UI 문제는 한 번에 큰 리팩터링보다 작은 수정과 검증을 반복
- 최종 문서는 초기 계획이 아니라 현재 구현 상태를 기준으로 작성

결국 AI는 초안 작성, 반복 수정, 구조화 설명에는 매우 강했지만, 실제 아키텍처의 진실성, 동작 검증, 마지막 판단은 사람이 계속 통제해야 했습니다.
