# ADR 0001: 모바일 미리보기(Epic D) 중단

## 상태

중단됨 (2026-08-26)

## 배경

`docs/instruction/0812_Admin_Web_Implementation_Spec.md` §16 "모바일 미리보기"에 따라, 관리자 웹(naroom-admin)에서 naroom-app의 Expo Web 빌드를 iframe으로 띄워 발행 전 DRAFT 콘텐츠를 실제 앱 컴포넌트로 미리 볼 수 있게 하는 기능(Epic D)을 단계적으로 구현했다.

## 구현된 범위

- **D-1** (naroom-app): Expo Web native-only 의존성 분리. `expo-secure-store`의 웹 미구현(`getValueWithKeyAsync`)으로 부팅이 멈추던 문제를 `src/lib/secureStorage.ts` 플랫폼 어댑터로 해결.
- **D-2** (naroom-api): preview 세션 인증 기반. `preview_sessions` 테이블, `X-Preview-Token` 헤더 기반 전용 보안 필터체인(`com.naroom.api.preview.*`), 관리자 세션과 동일한 불투명 토큰 패턴.
- **D-3** (naroom-api + naroom-app): "오늘의 문장" 미리보기. `GET /api/v1/preview/content/quotes/today`, 기존 `QuoteService` 재사용. naroom-app `/preview/quote` 라우트.
- **D-4** (naroom-api + naroom-app): 작은 실험 코스 상세(카탈로그 열람) 미리보기. `GET /api/v1/preview/content/experiment-programs`, 기존 `ExperimentProgramService` 재사용. naroom-app `/preview/experiment-program` 라우트.
- **D-5** (naroom-admin + naroom-app): iframe 임베드, postMessage handshake(토큰을 URL에 남기지 않고 iframe 준비 완료 신호 이후 전달), iframe sandbox 최소 권한, naroom-admin `frame-src` CSP. 실사용 중 발견된 postMessage 타이밍 레이스(iframe onLoad와 리스너 등록 순서 문제)를 ready-신호 기반 handshake로 수정.

세션 발급 → iframe 임베드 → postMessage handshake → 콘텐츠 렌더링까지 핵심 파이프라인은 end-to-end로 동작을 확인했다(단, 완전한 사람이 직접 보는 종단 확인은 관리자 OAuth 로그인 제약으로 자동화 검증 범위 밖).

## 남겨둔 채 중단한 하위 작업

다음은 이슈로 만들었다가 이번 결정으로 함께 정리(삭제)했다. 필요해지면 이 문서를 참고해 다시 스코핑한다.

- **D-3b**: 질문(record prompt) 미리보기 — 회원용 질문 조회 API 자체가 없어(현재 앱은 하드코딩된 로컬 목록에서 랜덤 선택) 그린필드 설계 필요.
- **D-4b**: 작은 실험 진행 상태 시뮬레이션(3일 코스 Day 1/2/3 등) — synthetic Member/UserExperimentProgram 인프라 필요.
- **D-5b**: preview 쪽 `frame-ancestors` CSP·호스팅 결정 — naroom-app이 정적 SPA export라 호스팅 플랫폼이 정해져야 서버 헤더 설정 가능.
- **D-5c**: 시나리오(첫 방문/코스 진행 등) 선택 UI — D-3b/D-4b(synthetic member) 선행 필요.

## 결정

사용자가 실제 동작을 확인한 뒤, 미리보기 기능이 애초에 의도했던 것과 다르다고 판단해 여기서 중단하기로 했다("미리보기 기능 자체가 내가 생각했던 것과 다른 것 같아"). 구체적으로 무엇이 기대와 달랐는지는 이 시점에 기록되지 않았다 — 재개하기 전에 사용자와 다시 요구사항을 확인해야 한다.

이미 머지된 코드(D-1~D-5, 아래 PR 목록)는 되돌리지 않고 `dev`/`main`에 그대로 남겨둔다. 관련 GitHub 이슈는 삭제했다(추적할 남은 작업이 없어짐). PR은 GitHub이 삭제를 지원하지 않아 병합된 상태로 남는다.

## 참고: 관련 PR (병합됨, 코드는 여전히 dev/main에 존재)

- narrom-api: #56(D-2 문서상 이슈 참고용, 실제 PR은 #57), #57(D-2), #58(D-3), #60(D-4), #55(무관 - 코드 자동 부여·정렬)
- naroom-app: #36(D-1), #37(D-3), #39(D-4), #41(D-5 handshake), #44(handshake 타이밍 수정)
- naroom-admin: #4(D-5), #7(handshake 타이밍 수정)

## 재개 시 참고 사항

- preview 인증·세션 인프라(D-2)와 문장·코스 미리보기(D-3/D-4)는 그대로 재사용 가능한 상태로 남아 있다.
- 재개 전에 반드시 "미리보기 기능이 실제로 어떤 사용자 흐름/화면을 재현해야 하는가"를 사용자와 먼저 합의한다 — 이번 중단의 원인이 정확히 이 부분의 기대 불일치였다.
