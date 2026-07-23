# 공통 오류 응답

상태: Beta 1 계약안

## 기본 형식

Spring `ProblemDetail` 기반 RFC 9457 형식을 사용한다.

```json
{
  "type": "urn:naroom:problem:auth-access-token-expired",
  "title": "인증 정보가 만료되었습니다",
  "status": 401,
  "detail": "인증 정보를 다시 확인해 주세요.",
  "instance": "/api/v1/auth/session",
  "code": "AUTH_ACCESS_TOKEN_EXPIRED",
  "stage": "TOKEN",
  "action": "REFRESH_REQUIRED",
  "retryable": false,
  "timestamp": "2026-07-23T09:30:15.123Z",
  "traceId": "01K0WQXW9F6XJ3KBKWBZ6P4D7V"
}
```

`title`/`detail`은 서버가 채우는 fallback 문구다. 앱은 화면에 이 문자열을 그대로 노출하지 않고 `code` 기준으로 자체 로컬라이즈·브랜드 톤에 맞춘 문구를 사용한다. `title`/`detail`은 앱 로그가 없는 상황의 디버깅·개발자 콘솔 표시용으로만 취급한다.

## 단계별 식별 필드

| 필드 | 역할 |
|---|---|
| `code` | 앱 분기와 테스트에 사용하는 고정 오류 코드 |
| `stage` | 실패한 처리 단계 |
| `action` | 클라이언트의 다음 복구 동작 |
| `retryable` | **동일한 요청을 그대로(사용자 조작 없이) 다시 보내면 성공할 가능성이 있는지.** `action`이 `RETRY` 이외의 값이면(재로그인, 재발급 등 클라이언트 쪽 조치가 먼저 필요하면) 대부분 `false`다. |
| `traceId` | 서버 로그 추적용 ID |

`title`과 `detail` 문구가 바뀌어도 앱은 반드시 `code`, `stage`, `action`으로 분기한다.

### stage

```text
REQUEST
DEVICE
LOGIN
TOKEN
SESSION
ACCOUNT
ONBOARDING
PERSISTENCE
EXTERNAL
INTERNAL
```

### action

```text
NONE
RETRY
CHECK_REQUEST
CHECK_DEVICE
LOGIN_REQUIRED
REFRESH_REQUIRED
CLEAR_SESSION_AND_LOGIN
COMPLETE_ONBOARDING
CONFIRM_ACCOUNT_RECOVERY
RELOAD_RESOURCE
CONTACT_SUPPORT
```

## 인증·시작 단계 오류

| 코드 | HTTP | 단계 | action | 의미 |
|---|---:|---|---|---|
| `DEVICE_INSTALLATION_KEY_REQUIRED` | 400 | DEVICE | CHECK_DEVICE | 설치 식별자 누락 |
| `DEVICE_PLATFORM_UNSUPPORTED` | 400 | DEVICE | CHECK_DEVICE | 지원하지 않는 플랫폼 |
| `AUTH_KAKAO_TOKEN_INVALID` | 401 | LOGIN | LOGIN_REQUIRED | 카카오 인증 정보가 유효하지 않음 |
| `AUTH_KAKAO_UNAVAILABLE` | 503 | EXTERNAL | RETRY | 카카오 서비스 일시 장애 |
| `AUTH_SOCIAL_IDENTITY_REVOKED` | 403 | LOGIN | CONTACT_SUPPORT | 소셜 로그인 연결이 비활성화됨 |
| `AUTH_REQUIRED` | 401 | TOKEN | LOGIN_REQUIRED | Access Token 없음 |
| `AUTH_ACCESS_TOKEN_EXPIRED` | 401 | TOKEN | REFRESH_REQUIRED | Access Token 만료 |
| `AUTH_ACCESS_TOKEN_INVALID` | 401 | TOKEN | CLEAR_SESSION_AND_LOGIN | Access Token 위조·형식 오류 |
| `AUTH_REFRESH_TOKEN_EXPIRED` | 401 | TOKEN | CLEAR_SESSION_AND_LOGIN | Refresh Token 만료 |
| `AUTH_REFRESH_TOKEN_INVALID` | 401 | TOKEN | CLEAR_SESSION_AND_LOGIN | 현재 세션 해시와 불일치 |
| `AUTH_DEVICE_MISMATCH` | 401 | DEVICE | CLEAR_SESSION_AND_LOGIN | 세션과 설치 기기가 일치하지 않음 |
| `AUTH_SESSION_NOT_FOUND` | 401 | SESSION | CLEAR_SESSION_AND_LOGIN | 세션 없음 |
| `AUTH_SESSION_EXPIRED` | 401 | SESSION | CLEAR_SESSION_AND_LOGIN | 세션 만료 |
| `AUTH_SESSION_REVOKED` | 401 | SESSION | CLEAR_SESSION_AND_LOGIN | 로그아웃·탈퇴 등으로 폐기된 세션 |
| `AUTH_FORBIDDEN` | 403 | ACCOUNT | CONTACT_SUPPORT | 인증은 됐지만 해당 리소스에 대한 권한이 없음(Spring Security `AccessDeniedException`) |
| `ACCOUNT_LOCKED` | 403 | ACCOUNT | CONTACT_SUPPORT | 잠긴 회원 |
| `ACCOUNT_PENDING_DELETION` | 409 | ACCOUNT | CONFIRM_ACCOUNT_RECOVERY | 삭제 대기 회원 |
| `ACCOUNT_RECOVERY_NOT_AVAILABLE` | 409 | ACCOUNT | CONTACT_SUPPORT | 삭제 기한 경과 등으로 복구할 수 없음 |
| `ONBOARDING_REQUIRED` | 409 | ONBOARDING | COMPLETE_ONBOARDING | 보호 기능 사용 전 온보딩 필요 |
| `ONBOARDING_CONSENT_REQUIRED` | 400 | ONBOARDING | CHECK_REQUEST | 필수 동의 누락 |
| `ONBOARDING_DOCUMENT_VERSION_INVALID` | 409 | ONBOARDING | CHECK_REQUEST | 현재 허용하지 않는 문서 버전 |

## 삭제 대기 오류

```json
{
  "type": "urn:naroom:problem:account-pending-deletion",
  "title": "계정이 삭제 대기 중입니다",
  "status": 409,
  "detail": "삭제 취소 여부를 확인해 주세요.",
  "instance": "/api/v1/auth/kakao/login",
  "code": "ACCOUNT_PENDING_DELETION",
  "stage": "ACCOUNT",
  "action": "CONFIRM_ACCOUNT_RECOVERY",
  "retryable": false,
  "timestamp": "2026-07-23T09:30:15.123Z",
  "traceId": "01K0WQXW9F6XJ3KBKWBZ6P4D7V",
  "context": {
    "scheduledDeletionAt": "2026-07-30T09:30:15.123Z"
  }
}
```

`context`에는 오류 복구에 필요한 허용 필드만 넣는다. 회원 ID, 이메일, 카카오 ID는 넣지 않는다.

## 낙관적 잠금 충돌

`version`을 포함해 회원 정보를 직접 수정하는 API(프로필, timezone·locale, 온보딩 완료, 탈퇴 철회 등)에 적용한다. 전체 규칙은 [conventions.md](conventions.md#낙관적-잠금버전-충돌)를 따른다.

```json
{
  "type": "urn:naroom:problem:account-version-conflict",
  "title": "회원 정보가 변경되었습니다",
  "status": 409,
  "detail": "최신 정보를 다시 불러온 뒤 수정해 주세요.",
  "instance": "/api/v1/account/onboarding/complete",
  "code": "ACCOUNT_VERSION_CONFLICT",
  "stage": "PERSISTENCE",
  "action": "RELOAD_RESOURCE",
  "retryable": false,
  "timestamp": "2026-07-23T09:30:15.123Z",
  "traceId": "01K0WQXW9F6XJ3KBKWBZ6P4D7V"
}
```

`retryable: false`다. 자동 재시도를 허용하면 최신 데이터를 확인하지 않고 다른 변경을 덮어쓸 수 있다. 구현에서는 `ObjectOptimisticLockingFailureException`/`OptimisticLockException`만 이 코드로 변환하고, 그 외 DB 예외는 `COMMON_INTERNAL_ERROR`로 처리한다.

## 검증 오류

```json
{
  "type": "urn:naroom:problem:validation-failed",
  "title": "요청 내용을 확인해 주세요",
  "status": 400,
  "detail": "입력한 값 중 확인이 필요한 항목이 있습니다.",
  "instance": "/api/v1/account/onboarding/complete",
  "code": "COMMON_VALIDATION_FAILED",
  "stage": "REQUEST",
  "action": "CHECK_REQUEST",
  "retryable": false,
  "timestamp": "2026-07-23T09:30:15.123Z",
  "traceId": "01K0WQXW9F6XJ3KBKWBZ6P4D7V",
  "violations": [
    {
      "field": "consents",
      "code": "REQUIRED",
      "message": "필수 동의를 확인해 주세요."
    }
  ]
}
```

`violations`에는 사용자가 입력한 실제 값인 `rejectedValue`를 넣지 않는다.

## 공통 오류

특정 도메인 단계에 속하지 않는 cross-cutting 오류 코드.

| 코드 | HTTP | 단계 | action | 의미 |
|---|---:|---|---|---|
| `COMMON_VALIDATION_FAILED` | 400 | REQUEST | CHECK_REQUEST | 요청 형식·Bean Validation 실패 |
| `COMMON_RATE_LIMITED` | 429 | REQUEST | RETRY | 요청 빈도 제한 초과 |
| `COMMON_INTERNAL_ERROR` | 500 | INTERNAL | CONTACT_SUPPORT | 분류되지 않은 서버 오류 |

`COMMON_RATE_LIMITED`는 응답 헤더 `Retry-After`(초 단위)와 `context.retryAfterSeconds`를 함께 반환한다. 적용 대상 endpoint는 [authentication.md](authentication.md#요청-제한rate-limit)를 따른다.

```json
{
  "type": "urn:naroom:problem:common-rate-limited",
  "title": "요청이 너무 많습니다",
  "status": 429,
  "detail": "잠시 후 다시 시도해 주세요.",
  "instance": "/api/v1/auth/refresh",
  "code": "COMMON_RATE_LIMITED",
  "stage": "REQUEST",
  "action": "RETRY",
  "retryable": true,
  "timestamp": "2026-07-23T09:30:15.123Z",
  "traceId": "01K0WQXW9F6XJ3KBKWBZ6P4D7V",
  "context": {
    "retryAfterSeconds": 30
  }
}
```

`COMMON_INTERNAL_ERROR`는 예상하지 못한 서버 오류의 catch-all이다. 실제 예외 메시지, stack trace, SQL, 제약조건명은 응답에 포함하지 않는다(서버 로그에만 `traceId`와 함께 남긴다).

## ERD 제약

현재 `auth_sessions`는 현재 `refresh_token_hash` 하나만 저장한다. 이전 토큰 해시나 토큰 패밀리 이력이 없으므로 Beta 1 계약에서는 재사용 토큰과 임의의 잘못된 토큰을 별도 판별하지 않는다.

- 둘 다 `AUTH_REFRESH_TOKEN_INVALID`로 응답
- 해당 세션을 보안상 폐기
- `AUTH_REFRESH_TOKEN_REUSED` 코드는 현재 사용하지 않음

향후 재사용 탐지를 별도로 제공하려면 ERD에 토큰 패밀리 또는 회전 이력 구조가 먼저 추가되어야 한다.

## 처리 위치

동일한 오류 생성기를 다음 위치에서 함께 사용한다.

- `@RestControllerAdvice`
- Spring Security `AuthenticationEntryPoint`
- Spring Security `AccessDeniedHandler`
- JWT·세션 검증 필터

예외 stack trace, SQL, DB 제약조건명, 내부 클래스명은 응답하지 않는다.
