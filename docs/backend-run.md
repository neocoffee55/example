# Backend 실행 가이드

## 문서 범위

이 문서는 현재 구현 기준의 Tax Workbench 백엔드를 로컬에서 실행하는 방법을 설명합니다.

현재 백엔드는 Spring Boot 4 애플리케이션이며 다음 기능을 제공합니다.

- WorkItem 조회
- WorkItem 생성, 수정, 삭제
- WorkItem 변경로그 조회
- Client 조회
- Client 저장

로컬 데이터베이스는 in-memory H2를 사용합니다.

## 사전 준비

- Java 21 대상
- Maven 3.9+

참고:

- 프로젝트는 [backend/pom.xml](/Users/insu_han/IdeaProjects/example/backend/pom.xml)에서 Java 21을 기준으로 설정되어 있습니다.
- 로컬 머신에서 더 높은 버전의 JDK로 Maven을 실행할 수는 있지만, 기준 버전은 Java 21입니다.

## 위치

- 프로젝트 루트: `/Users/insu_han/IdeaProjects/example`
- 백엔드 모듈: `/Users/insu_han/IdeaProjects/example/backend`

## 실행 방법

```bash
cd /Users/insu_han/IdeaProjects/example/backend
mvn spring-boot:run
```

## 현재 런타임 기본값

- Backend URL: `http://localhost:8081`
- H2 JDBC URL: `jdbc:h2:mem:taxworkbench;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- H2 콘솔: 활성화
- JPA DDL 모드: `update`
- SQL 로그: 활성화

주요 설정 파일:

- [application.yml](/Users/insu_han/IdeaProjects/example/backend/src/main/resources/application.yml)

## 검증 방법

테스트 실행:

```bash
cd /Users/insu_han/IdeaProjects/example/backend
mvn test
```

헬스체크 확인:

```bash
curl http://localhost:8081/actuator/health
```

WorkItem 조회 확인:

```bash
curl "http://localhost:8081/api/work-items"
```

Client 조회 확인:

```bash
curl "http://localhost:8081/api/clients"
```

특정 WorkItem의 변경로그 확인:

```bash
curl "http://localhost:8081/api/work-items/WI-10031/audit-logs"
```

## API 요약

### WorkItem

- `GET /api/work-items`
- `POST /api/work-items`
- `POST /api/work-items/bulk`
- `PATCH /api/work-items/{id}`
- `DELETE /api/work-items/{id}?revision=...&changedBy=...`
- `GET /api/work-items/{id}/audit-logs`

### Client

- `GET /api/clients`
- `PUT /api/clients`

## 데이터 관련 참고

- 초기 데이터는 [SeedDataConfig.java](/Users/insu_han/IdeaProjects/example/backend/src/main/java/com/taxworkbench/bootstrap/SeedDataConfig.java)에서 적재합니다.
- WorkItem 데이터는 `work_item` 테이블에 저장됩니다.
- 변경로그는 `work_item_audit_log` 테이블에 별도로 저장됩니다.
- 신규 WorkItem 생성 시에는 현재 audit row를 남기지 않습니다.
- WorkItem 수정과 삭제 시에는 audit row를 남깁니다.

## 문제 해결

### 프론트에서 백엔드 호출이 안 되는 경우

프론트 개발 서버는 `http://localhost:8080`에서 실행되지만, `/api` 요청은 `http://localhost:8081`로 프록시됩니다.

프론트에서 `ERR_CONNECTION_REFUSED`가 보이면 먼저 백엔드가 `8081`에서 정상 실행 중인지 확인해야 합니다.

### SQL 로그가 너무 많은 경우

현재 백엔드는 로컬 디버깅을 위해 SQL과 bind 값을 콘솔에 출력합니다. 관련 설정은 [application.yml](/Users/insu_han/IdeaProjects/example/backend/src/main/resources/application.yml)에 있습니다.
