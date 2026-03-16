# Tax Workbench

## 1. 프로젝트 한 줄 설명

Tax Workbench는 세무 신고 시즌의 대량 업무를 빠르게 처리하기 위한 그리드 중심 운영 워크벤치입니다.  
이번 구현의 핵심은 "빠른 작업 흐름"과 "저장 정합성"을 동시에 확보하는 것입니다.

## 2. 왜 이 프로젝트를 만들었는가

세무 업무 운영 환경에서는 다음 문제가 반복됩니다.

- 한 명의 담당자가 많은 고객사 업무를 동시에 관리해야 함
- 업무 상태, 마감일, 담당자, 업무유형을 매우 빠르게 확인하고 수정해야 함
- 여러 사용자가 동시에 편집할 때 데이터 충돌이 발생할 수 있음
- 변경 이력을 추적해야 하지만, 그 때문에 메인 목록이 느려지면 안 됨

즉, 단순 CRUD 화면이 아니라 운영자가 "엑셀처럼 빠르게 보고, 수정하고, 저장하는" 워크벤치가 필요합니다.

## 3. 이번 구현에서 보여주고자 한 것

이번 버전은 완성형 제품이 아니라, 아래 항목이 실제로 연결되어 동작하는 아키텍처 프로토타입입니다.

- H2 기반 데이터 저장
- WorkItem 조회 / 생성 / 수정 / 삭제
- optimistic locking 기반 충돌 방지
- WorkItem 변경로그 분리 조회
- Client 조회 및 선택 팝업
- 그리드 기반 인라인 편집
- 저장 후 토스트 피드백
- CSV Export

핵심은 "화면만 있는 데모"가 아니라, 프론트와 백엔드가 데이터베이스를 기준으로 end-to-end로 연결되어 있다는 점입니다.

## 4. 현재 데모 시나리오

발표 시에는 아래 흐름으로 설명하는 것이 가장 자연스럽습니다.

### 시나리오 A. 기본 조회와 작업 흐름

- 화면 진입 시 첫 번째 WorkItem이 자동 선택됨
- 왼쪽 메인 그리드에 WorkItem 목록 표시
- 오른쪽 패널에 선택된 WorkItem의 변경로그 표시

### 시나리오 B. 필터 기반 조회

- 상단에서 `업체명`, `상태`, `담당자`, `마감일` 기준으로 조회
- 조회 결과는 메인 그리드에 반영
- 데이터 로드 후 첫 번째 행이 다시 활성화되고 변경로그도 자동 조회

### 시나리오 C. 데이터 추가

- `add` 클릭
- 새 WorkItem 행 추가
- `법인정보` 팝업에서 Client 선택
- 선택한 `id / name / bizNo`가 새 WorkItem에 반영

### 시나리오 D. 데이터 수정과 저장

- 메인 그리드에서 업무유형, 마감일 등 수정
- `save` 클릭
- 신규 행은 bulk insert로 저장
- 수정/삭제는 row 단위로 저장
- 저장 완료 후 하단 가운데 토스트 표시
- 저장 후 데이터 재조회

### 시나리오 E. 변경로그 확인

- 수정된 WorkItem 행 선택
- 오른쪽 변경로그 그리드에서 변경 컬럼, 이전값, 이후값, 변경시각 확인

## 5. 아키텍처를 이렇게 잡은 이유

### Backend

백엔드는 다음 경계를 유지합니다.

- `application`
- `infrastructure.persistence`
- `interfaces.http`
- `bootstrap`
- `domain`

아직 모든 도메인 규칙이 분리된 것은 아니지만, 최소한 아래 원칙은 유지하고 있습니다.

- 컨트롤러는 얇게 유지
- 저장 orchestration은 서비스 레이어에 집중
- audit log는 메인 조회 경로와 분리
- WorkItem 저장은 row 단위 API로 분리
- 충돌 판단은 백엔드가 책임짐

이 구조를 택한 이유는, 초기 구현 속도를 유지하면서도 나중에 `use case`, `policy`, `domain rule`로 확장할 수 있는 기반을 남기기 위해서입니다.

### Frontend

프론트는 운영자 경험에 집중했습니다.

- 왼쪽: WorkItem 메인 그리드
- 오른쪽: 변경로그 그리드
- 상단: 빠른 조회 및 액션 버튼
- 팝업: Client 선택과 관리

TanStack Table을 사용한 이유는 셀, 헤더, 정렬, 페이징, 편집 흐름을 세밀하게 직접 제어할 수 있기 때문입니다. 지금 단계에서는 무거운 완성형 그리드보다 "업무 흐름 맞춤 제어"가 더 중요했습니다.

## 6. 정합성을 어떻게 보장하는가

이번 구현의 핵심 포인트 중 하나는 저장 충돌을 단순히 무시하지 않는다는 점입니다.

- WorkItem은 `revision`을 가짐
- 수정과 삭제는 현재 revision을 기준으로 처리
- 서버 revision과 다르면 `409 Conflict` 반환
- 충돌 응답에는 `entityId`, `serverRevision`, `conflictFields`, `serverSnapshot`, `attemptedChanges` 포함

즉, 여러 사용자가 동시에 수정하더라도 마지막 저장이 무조건 덮어쓰는 구조가 아니라, 충돌을 감지할 수 있는 구조입니다.

## 7. 변경로그를 따로 뺀 이유

변경로그는 운영상 매우 중요하지만, 메인 목록과 한 쿼리로 묶으면 목록 성능을 해칠 수 있습니다.  
그래서 현재 구조는 다음 원칙을 따릅니다.

- WorkItem 데이터는 `work_item`
- 변경 이력은 `work_item_audit_log`
- 변경로그는 선택된 WorkItem 기준 별도 조회

이렇게 하면 이력 데이터가 커져도 메인 조회의 책임과 분리할 수 있습니다.

## 8. 현재 설계의 장점과 한계

### 장점

- 로컬에서 바로 실행 가능한 전체 흐름
- DB 기반 저장과 변경로그 조회
- 인라인 편집 중심의 빠른 사용자 경험
- 신규 bulk insert + 수정/삭제 row 단위 저장 구조
- 충돌 감지 가능

### 한계

- WorkItem 조회가 아직 DB 주도 페이징이 아님
- Client 저장은 아직 sync 방식
- 프론트 메인 화면이 큰 단일 파일에 집중됨
- Export는 스트리밍 응답이지만, 조회 필터링은 아직 메모리 기반임

즉, 현재 버전은 "운영 구조의 핵심을 보여주는 프로토타입"이며, 대규모 운영 대응은 다음 단계에서 확장해야 합니다.

## 9. 운영 환경으로 확장할 때의 방향

프로덕션으로 가면 현재 구조는 다음 방향으로 확장하는 것이 맞습니다.

- DB: H2에서 PostgreSQL 같은 관리형 RDBMS로 전환
- API: 목록 조회를 DB projection + 정렬 + 페이징 구조로 전환
- Export: 현재 streaming CSV에서 비동기 Export Job + Object Storage 다운로드 링크로 전환
- Bulk Insert: 현재 동기 chunk 처리에서 queue 기반 비동기 처리로 전환
- Audit: append-only 유지, 필요 시 비동기 저장 경로 검토
- 인프라: Container Apps, Service Bus, Functions, Blob Storage 같은 PaaS 조합으로 확장

핵심 전략은 "운영자가 기다리는 동기 API는 얇고 빠르게 유지하고, 무거운 작업은 비동기 워커로 분리하는 것"입니다.

## 10. 실행 방법 요약

상세 실행 방법은 아래 문서를 참고하면 됩니다.

- [docs/backend-run.md](/Users/insu_han/IdeaProjects/example/docs/backend-run.md)
- [docs/frontend-run.md](/Users/insu_han/IdeaProjects/example/docs/frontend-run.md)
- [docs/api-spec.md](/Users/insu_han/IdeaProjects/example/docs/api-spec.md)

요약만 적으면:

### Backend

```bash
cd /Users/insu_han/IdeaProjects/example/backend
mvn spring-boot:run
```

- `http://localhost:8081`

### Frontend

```bash
cd /Users/insu_han/IdeaProjects/example/frontend
npm install
npm run dev
```

- `http://localhost:8080`

## 11. AI 협업 관점에서의 회고

이번 작업에서 AI는 빠른 구현 파트너로는 매우 유용했습니다.

- 초기 구조 세팅
- 반복적인 UI 수정
- 문서 정리
- API 흐름 연결

반면, 그대로 믿을 수는 없었습니다.

- UI 회귀가 반복적으로 발생할 수 있었고
- 문서가 실제 런타임 설정과 어긋나는 경우가 있었으며
- 구조 설명이 실제 구현보다 앞서 나가는 문제도 있었습니다.

그래서 다음 원칙으로 보완했습니다.

- `mvn test`, `npm run build` 반복 검증
- 실제 코드 기준 문서 재작성
- 포트, 프록시, API 흐름을 소스에서 다시 확인

결론적으로 AI는 구현 속도를 높이는 데 강했지만, 최종 구조 판단과 품질 통제는 사람이 계속 잡아야 했습니다.
