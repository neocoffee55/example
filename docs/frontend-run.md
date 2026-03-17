# Frontend 실행 가이드

## 문서 범위

이 문서는 현재 구현 기준의 Tax Workbench 프론트엔드를 로컬에서 실행하는 방법을 설명합니다.

현재 프론트는 React 19 + TypeScript + Tailwind CSS 기반의 워크벤치 UI이며, 다음 기능을 포함합니다.

- 왼쪽 WorkItem 그리드
- 오른쪽 변경로그 그리드
- 상단 필터/액션 바
- 인라인 셀 편집
- 법인정보 팝업 그리드
- 저장 완료 토스트

## 사전 준비

- Node.js 22.12+ 권장
- npm 10+

참고:

- 현재 로컬 머신의 Node 버전은 `22.11.0`입니다.
- `npm run build`는 성공하지만, Vite가 `22.12+`를 권장한다는 경고를 출력할 수 있습니다.

## 위치

- 프로젝트 루트: `/Users/insu_han/IdeaProjects/example`
- 프론트 모듈: `/Users/insu_han/IdeaProjects/example/frontend`

## 의존성 설치

```bash
cd /Users/insu_han/IdeaProjects/example/frontend
npm install
```

## 개발 서버 실행

```bash
cd /Users/insu_han/IdeaProjects/example/frontend
npm run dev
```

현재 런타임 기본값:

- Frontend URL: `http://localhost:8082`
- `/api` 프록시 대상: `http://localhost:8081`

주요 개발 서버 설정 파일:

- [vite.config.ts](/Users/insu_han/IdeaProjects/example/frontend/vite.config.ts)

## 프로덕션 빌드

```bash
cd /Users/insu_han/IdeaProjects/example/frontend
npm run build
```

## 프로덕션 빌드 미리보기

```bash
cd /Users/insu_han/IdeaProjects/example/frontend
npm run preview
```

## 주요 소스 파일

- 앱 진입점: [main.tsx](/Users/insu_han/IdeaProjects/example/frontend/src/main.tsx)
- 메인 화면: [WorkbenchShell.tsx](/Users/insu_han/IdeaProjects/example/frontend/src/workbench/WorkbenchShell.tsx)
- 전역 스타일: [styles.css](/Users/insu_han/IdeaProjects/example/frontend/src/styles.css)

## 현재 UI 동작

### 메인 WorkItem 화면

- 상단 검색/필터 버튼: `업체명`, `상태`, `담당자`, `마감일`
- 액션 버튼: `법인정보`, `find`, `add`, `del`, `save`, `Export`
- 정렬, 인라인 수정, 체크박스 선택, 페이징이 가능한 WorkItem 그리드
- 데이터 로드 시 첫 번째 행 자동 포커스 및 변경로그 자동 조회
- 선택된 WorkItem에 따라 다시 조회되는 우측 변경로그 패널

### 법인정보 팝업

- Client 조회, 추가, 삭제, 저장이 가능한 팝업 그리드
- 선택한 Client를 WorkItem에 연결하는 흐름
- 데이터가 많아지면 데이터 영역만 스크롤
- 팝업 저장 완료 시에도 하단 가운데 토스트 표시

### Export

- 현재 적용된 조회 조건을 기준으로 백엔드 CSV streaming export를 실행합니다.
- 프론트는 `/api/work-items/export`를 호출하고, 백엔드가 내려준 CSV 파일을 다운로드합니다.

## 검증 방법

프론트와 백엔드를 모두 실행한 뒤 다음을 확인합니다.

1. `http://localhost:8082` 접속
2. WorkItem 데이터가 표시되는지 확인
3. 행을 클릭했을 때 오른쪽 변경로그 그리드가 바뀌는지 확인
4. `법인정보` 팝업을 열고 데이터가 로드되는지 확인
5. 메인 또는 팝업에서 `save` 후 하단 가운데 토스트가 나타나는지 확인

## 문제 해결

### `/api/work-items` 호출 시 `ERR_CONNECTION_REFUSED`

프론트는 API 데이터를 직접 제공하지 않고, `/api` 요청을 `http://localhost:8081`로 프록시합니다.

다음을 확인해야 합니다.

- 백엔드가 실행 중인지
- 백엔드가 `8081` 포트에서 listening 중인지
- Vite 개발 서버가 `8082` 포트에서 실행 중인지

### Node 버전 경고

Vite가 현재 Node 버전에 대해 경고하면 `22.12+`로 올리는 것이 좋습니다. 현재 프로젝트는 `22.11.0`에서도 빌드는 되지만 경고는 유지됩니다.

### 화면은 뜨는데 데이터가 바뀌지 않는 경우

브라우저 네트워크 탭과 백엔드 콘솔을 같이 확인합니다.

- 프론트 요청은 `/api/...`로 나가야 합니다.
- 백엔드 콘솔에는 현재 SQL 로그가 출력되어야 합니다.
