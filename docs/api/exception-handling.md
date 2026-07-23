# 예외 처리 구조

상태: Beta 1 구현 기준안  
클라이언트 오류 계약: [error-response.md](error-response.md)

## 목표

모든 실패는 발생 위치가 달라도 같은 `ProblemDetail` 계약으로 수렴한다.

```text
요청 파싱·검증
도메인·애플리케이션 규칙
JWT·Spring Security
DB 제약·동시성
카카오 외부 API
예상하지 못한 서버 오류
        ↓
ErrorCode로 분류
        ↓
ProblemDetailFactory
        ↓
application/problem+json
```

Controller에서 예외를 직접 잡아 응답을 만들지 않는다.

## 권장 패키지

실제 base package는 현재 프로젝트 값을 유지한다.

```text
global
├─ error
│  ├─ code
│  │  ├─ ErrorCode
│  │  ├─ ErrorStage
│  │  ├─ ClientAction
│  │  └─ CommonErrorCode
│  ├─ exception
│  │  └─ BusinessException
│  ├─ response
│  │  ├─ ProblemDetailFactory
│  │  └─ ValidationViolation
│  └─ handler
│     └─ GlobalExceptionHandler
├─ security
│  ├─ ApiAuthenticationEntryPoint
│  ├─ ApiAccessDeniedHandler
│  └─ SecurityProblemWriter
└─ web
   └─ RequestTraceFilter

auth
└─ security
   ├─ JwtProperties
   ├─ JwtTokenProvider (Access Token 발급·검증)
   ├─ RefreshTokenGenerator (Refresh Token 생성·해시)
   ├─ AccessTokenClaims
   ├─ MemberAuthentication
   └─ JwtAuthenticationFilter
```

기능별 오류 코드는 각 기능이 소유한다.

```text
auth/domain/error/AuthErrorCode
account/domain/error/AccountErrorCode
record/domain/error/RecordErrorCode
experiment/domain/error/ExperimentErrorCode
```

`global`이 기능 패키지를 참조하지 않도록 기능별 ErrorCode enum이 공통 `ErrorCode` 인터페이스를 구현한다.

## 핵심 타입 책임

### `ErrorCode`

오류의 고정 계약을 제공한다.

```text
httpStatus
code
type
title
detail
stage
action
retryable
```

- `code`, HTTP 상태, `stage`, `action`은 배포 후 임의 변경하지 않는다.
- Exception의 동적 메시지를 `detail`로 사용하지 않는다.
- 기능별 enum은 응답 계약만 소유하고 처리 로직을 포함하지 않는다.

### `BusinessException`

예상 가능한 도메인·애플리케이션 오류에 사용한다.

```java
throw new BusinessException(AccountErrorCode.PENDING_DELETION);
```

원칙:

- `RuntimeException`
- 필수 값은 `ErrorCode`
- 필요하면 오류 코드별로 허용된 안전한 context만 포함
- Controller나 Repository가 아니라 도메인 정책·Application Service에서 주로 발생
- 예외 메시지와 cause를 응답에 노출하지 않음

### `JwtAuthenticationFilter`의 실패 전달 방식

Controller 이전(Security Filter Chain)에서 발생한 인증 실패는 예외를 던지지 않고 request attribute로 전달한다.

- 이 필터는 어떤 경로가 `permitAll`인지 알지 못한 채로(`AuthorizationFilter`보다 먼저) 실행된다. 여기서 예외를 던지면 공개 경로에 유효하지 않은 토큰이 섞여 들어와도 막혀버린다.
- 대신 실패 사유(`ErrorCode`)를 `request.setAttribute(JwtAuthenticationFilter.AUTH_FAILURE_ATTRIBUTE, ...)`로 남기고 체인을 계속 진행시킨다.
- 실제로 인증이 필요한 경로에서만 `ApiAuthenticationEntryPoint`가 이 attribute를 읽어 `AUTH_ACCESS_TOKEN_EXPIRED`/`AUTH_SESSION_REVOKED` 등 구체적인 코드로 응답한다. attribute가 없으면(토큰 자체가 없었으면) `AUTH_REQUIRED`로 기본 처리한다.

### `ProblemDetailFactory`

모든 Handler가 공유하는 단일 오류 응답 생성기다.

책임:

- RFC 9457 기본 필드 생성
- `code`, `stage`, `action`, `retryable`, `timestamp`, `traceId` 추가
- `instance`에서 query string 제거
- 허용된 `violations`, `context`만 추가
- Content-Type을 `application/problem+json`으로 통일
- 실제 예외 메시지와 민감정보 차단

### `GlobalExceptionHandler`

`@RestControllerAdvice`이자 `ResponseEntityExceptionHandler` 확장 클래스로 구현한다.

담당:

- `BusinessException`
- `MethodArgumentNotValidException`
- `HandlerMethodValidationException`
- `ConstraintViolationException`
- `HttpMessageNotReadableException`
- `HttpRequestMethodNotSupportedException`
- `HttpMediaTypeNotSupportedException`
- 예상하지 못한 `Exception`

Spring MVC 기본 예외도 `ProblemDetailFactory`를 거쳐 동일한 확장 필드를 갖게 한다.

## 계층별 발생·변환 규칙

| 발생 위치 | 원본 실패 | 변환 위치 | 최종 처리 |
|---|---|---|---|
| Controller 바인딩 | JSON·Bean Validation | `GlobalExceptionHandler` | `COMMON_*` |
| Domain/Application | 비즈니스 규칙 위반 | `BusinessException(ErrorCode)` | `GlobalExceptionHandler` |
| JWT 필터 | 토큰 없음·만료·위조 | request attribute(`AUTH_FAILURE_ATTRIBUTE`)에 `ErrorCode` 저장, 예외 던지지 않음 | `ApiAuthenticationEntryPoint` |
| Spring Security | 인증 후 권한 부족 | `AccessDeniedException` | `ApiAccessDeniedHandler` |
| Persistence | unique·상태 충돌 | Application/adapter에서 의미 있는 ErrorCode로 변환 | `GlobalExceptionHandler` |
| Persistence | `ObjectOptimisticLockingFailureException`/`OptimisticLockException` | `GlobalExceptionHandler`에서 직접 `ACCOUNT_VERSION_CONFLICT`로 변환 | `GlobalExceptionHandler` |
| 요청 제한 Interceptor/필터 | 초과 요청 | `BusinessException(COMMON_RATE_LIMITED)` | `GlobalExceptionHandler` |
| 카카오 Client | 401·429·5xx·timeout | auth infrastructure에서 내부 Auth ErrorCode로 변환 | `GlobalExceptionHandler` |
| 분류되지 않은 오류 | Runtime Exception | catch-all Handler | `COMMON_INTERNAL_ERROR` |

하위 계층의 라이브러리 예외가 Controller까지 그대로 올라오지 않게 한다.

## 단계별 인증 예외 흐름

### 카카오 로그인

```text
REQUEST: JSON·DTO 검증
→ DEVICE: installationKey·platform 검증
→ EXTERNAL/LOGIN: 카카오 토큰 검증
→ LOGIN: SocialIdentity 상태 확인
→ ACCOUNT: Member 상태 확인
→ PERSISTENCE: 회원·기기·세션 저장 충돌
→ TOKEN: 토큰 발급
```

### 토큰 재발급

```text
REQUEST: 요청 DTO 검증
→ TOKEN: Refresh Token 형식·만료 확인
→ SESSION: AuthSession 조회·상태 확인
→ DEVICE: installationKey 일치 확인
→ TOKEN: refresh_token_hash 비교
→ ACCOUNT: Member 상태 확인
→ PERSISTENCE: 세션 잠금·토큰 회전
```

### 앱 세션 확인

```text
TOKEN: Access Token 검증
→ SESSION: AuthSession 조회·상태 확인
→ DEVICE: DeviceInstallation 상태 확인
→ ACCOUNT: Member 상태 확인
→ ONBOARDING: onboarding_completed_at 판정
```

### 온보딩 완료

```text
TOKEN·SESSION: 인증 확인
→ ACCOUNT: ACTIVE 확인
→ REQUEST: 프로필·동의·문서 버전 검증
→ ONBOARDING: 필수 동의 확인
→ PERSISTENCE: 전체 온보딩 트랜잭션 저장
```

이 순서를 테스트 기준으로 사용하면 “로그인 실패” 하나로 뭉치지 않고 실패 지점을 구분할 수 있다.

## Spring Security 처리

`@RestControllerAdvice`는 Security Filter Chain에서 응답이 끝난 오류를 처리하지 못한다.

따라서 다음 경로를 별도로 둔다.

```text
JWT 인증 필터 (토큰 없음 또는 검증 실패)
→ request attribute에 실패 ErrorCode 기록 (예외 던지지 않음)
→ AuthorizationFilter가 해당 경로에 인증이 필요하다고 판단
→ ApiAuthenticationEntryPoint (attribute를 읽어 구체적인 코드 사용, 없으면 AUTH_REQUIRED)
→ SecurityProblemWriter
→ ProblemDetailFactory
```

```text
인증된 사용자의 권한 부족
→ AccessDeniedException
→ ApiAccessDeniedHandler
→ SecurityProblemWriter
→ ProblemDetailFactory
```

JWT 필터는 `UsernamePasswordAuthenticationFilter` 앞(`ExceptionTranslationFilter`보다도 앞)에 등록한다. 필터가 직접 임의 JSON을 작성하지 않는다 — 실패 판단과 응답 작성을 분리해서, 공개 경로(permitAll)에서는 실패해도 요청이 그대로 통과하게 한다.

구분:

- 인증 없음·실패: `401` + `AUTH_*`
- 인증 성공 후 권한 부족: `403` + `AUTH_FORBIDDEN`
- 타인의 민감 리소스: 존재 여부를 감추기 위해 기능별 `*_NOT_FOUND` `404`

## DB·트랜잭션 예외

DB 오류를 모두 `409`로 바꾸지 않는다.

처리 원칙:

1. 예상 가능한 unique·상태 충돌은 서비스 또는 persistence adapter에서 의미 있는 기능 오류로 변환한다.
2. 동시 Refresh Token 회전은 세션 행 잠금 또는 조건부 갱신으로 한 요청만 성공시킨다.
3. 온보딩 완료는 동의·프로필·알림·`onboarding_completed_at`을 하나의 트랜잭션으로 처리한다.
4. 카카오 네트워크 호출 중에는 DB 트랜잭션을 열어 두지 않는다.
5. `@Version`(`members.version`) 충돌로 발생한 `ObjectOptimisticLockingFailureException`/`OptimisticLockException`만 `ACCOUNT_VERSION_CONFLICT`로 변환한다. 다른 `DataIntegrityViolationException`까지 이 코드로 뭉뚱그리지 않는다.
6. 분류되지 않은 `DataIntegrityViolationException`은 내부 결함일 수 있으므로 무조건 409로 숨기지 않고 `COMMON_INTERNAL_ERROR`로 처리한다.
7. SQL, 제약조건명, 실제 DB 메시지는 로그의 제한된 내부 정보로만 취급하고 응답에는 포함하지 않는다.

## 외부 API 예외

카카오 Client는 외부 응답을 Naroom 오류 코드로 변환한다.

| 외부 상황 | 내부 코드 | HTTP |
|---|---|---:|
| 토큰 거부·사용자 확인 실패 | `AUTH_KAKAO_TOKEN_INVALID` | 401 |
| timeout·5xx·일시적 429 | `AUTH_KAKAO_UNAVAILABLE` | 503 |
| 예상하지 못한 응답 형식 | `AUTH_KAKAO_UNAVAILABLE` 또는 내부 오류로 기록 | 503 |

- 카카오 상태 코드와 원문 body를 앱에 그대로 전달하지 않는다.
- 외부 호출 timeout을 명시한다.
- 자동 재시도는 멱등하고 안전한 조회 요청에만 제한한다.
- Provider Token과 카카오 원본 응답을 로그에 남기지 않는다.

## 안전한 오류 context

`BusinessException`이 임의의 `Map<String, Object>`를 무제한으로 받지 않게 한다.

허용 예:

- `ACCOUNT_PENDING_DELETION`: `scheduledDeletionAt`
- 요청 제한: `retryAfterSeconds`

금지 예:

- `memberId`, 이메일, 카카오 사용자 ID
- 요청 DTO 전체
- 토큰과 해시
- Exception message
- 기록·감정·AI 원문

새 context 필드는 ErrorCode별 허용 목록과 OpenAPI 스키마를 함께 추가한다.

## 로깅

| 상황 | 권장 로그 |
|---|---|
| 일반 validation 400 | 필요 시 DEBUG 또는 집계 |
| 인증 실패 401·403 | 민감정보 없는 보안 이벤트 |
| 예상 가능한 409 | INFO 또는 WARN |
| 외부 서비스 503 | WARN, Provider 원문 제외 |
| 예상하지 못한 500 | ERROR + 내부 stack trace + traceId |

공통 규칙:

- 응답의 `traceId`와 로그의 traceId를 일치시킨다.
- 4xx를 모두 stack trace로 남기지 않는다.
- 500의 실제 원인은 서버 로그에만 남긴다.
- Authorization, query string, 요청 body 전체를 로깅하지 않는다.

## 구현 금지 사항

- Controller마다 `try-catch` 작성
- `Exception#getMessage()`를 `detail`로 반환
- `BusinessException` 하나에 HTTP 상태와 문자열을 매번 직접 전달
- 기능별 Handler가 서로 다른 오류 JSON 생성
- JWT 필터가 별도 JSON 문자열을 직접 조립
- 모든 DB 오류를 같은 `409`로 변환
- `ErrorResponse`라는 자체 DTO를 만들어 Spring의 `ErrorResponse` 타입과 혼동
- 이미 응답이 시작된 뒤 다시 오류 body 작성

## 최소 테스트

### 단위 테스트

- ErrorCode별 HTTP 상태·stage·action·retryable
- `ProblemDetailFactory`의 필드와 민감정보 차단
- Validation violation에 `rejectedValue`가 없는지
- 허용되지 않은 context가 제거되는지

### MVC 통합 테스트

- 잘못된 JSON
- Bean Validation 실패
- `BusinessException`
- 예상하지 못한 Exception
- 요청 제한 초과 시 `429` + `Retry-After` 헤더
- 모두 `application/problem+json`인지 확인

### Security 통합 테스트

- 토큰 없음
- Access Token 만료·위조
- 세션 없음·만료·폐기
- 기기 불일치·폐기
- 권한 부족
- 모든 경로가 같은 ProblemDetail 필드를 반환하는지 확인

### 인증 흐름 테스트

- 카카오 신규·기존 회원 로그인
- `ACTIVE` + 온보딩 완료·미완료
- `LOCKED`, `PENDING_DELETION`
- Refresh Token 해시 불일치 시 세션 폐기
- 동시 재발급 요청 중 하나만 성공
- 재발급 시점에 회원이 `LOCKED`·`PENDING_DELETION`이면 재발급이 거부됨
- 온보딩 중간 실패 시 `onboarding_completed_at`이 기록되지 않음
- 온보딩 완료 요청의 `version`이 현재 값과 다르면 `ACCOUNT_VERSION_CONFLICT`가 반환되고 저장되지 않음

## 구현 완료 조건

- 모든 오류 경로가 `ProblemDetailFactory`를 사용한다.
- 오류 응답에 `code`, `stage`, `action`, `retryable`, `traceId`가 있다.
- `@RestControllerAdvice`와 Security Handler의 JSON 구조가 같다.
- 최신 ERD로 판별할 수 없는 오류 코드를 만들지 않는다.
- 민감정보가 응답·테스트 스냅샷·일반 로그에 없다.
- OpenAPI에 공통 ProblemDetail과 주요 오류 응답이 정의되어 있다.
