# 인증·세션·온보딩 계약

상태: Beta 1 계약안  
구현 범위: 카카오·Google·Apple 로그인.

## ERD 기준

| 데이터 | 실제 기준 |
|---|---|
| 회원 상태 | `members.status`: `ACTIVE`, `LOCKED`, `PENDING_DELETION` |
| 온보딩 완료 | `members.onboarding_completed_at`의 null 여부 |
| 삭제 대기 | `members.withdrawal_requested_at`, `scheduled_deletion_at` |
| 소셜 로그인 | `social_identities.provider`, `provider_user_id`, `status` |
| 기기 | `device_installations.installation_key`, `platform`, `app_version`, `revoked_at` |
| 세션 | `auth_sessions.id`, `device_installation_id`, `refresh_token_hash`, `expires_at`, `revoked_at` |
| 필수 동의 | `member_consents`: `TERMS`, `PRIVACY`, `AI_PROCESSING` |

`scheduled_deletion_at`은 `PENDING_DELETION`일 때만 값이 있어야 한다.

## 토큰 정책

| 항목 | 값 |
|---|---|
| Access Token TTL | 1시간(`JWT_ACCESS_TOKEN_EXPIRATION`, 기본 3600000ms) |
| Refresh Token TTL | 14일(`JWT_REFRESH_TOKEN_EXPIRATION`, 기본 1209600000ms) |
| Refresh Token 저장 | `auth_sessions.refresh_token_hash`(단방향 해시, 원문 미저장) |
| Refresh 시 세션 유지 | 재발급해도 `auth_sessions.id`(`sid`)는 유지하고 `refresh_token_hash`만 교체한다(세션을 새로 만들지 않는다) |

위 TTL은 이번 계약의 확정 정책값이다. 아래 예시의 타임스탬프는 이 정책을 적용한 결과일 뿐이며, 예시 숫자 자체를 임의로 바꾸지 않는다.

## 앱 시작 판정 순서

### 1. 기기 확인

앱에서 먼저 다음 값을 확인하거나 생성한다.

- `installationKey`: 설치 단위 난수
- `platform`: `IOS`, `ANDROID` 등
- `appVersion`

익명 기기는 DB에 저장하지 않는다. `device_installations.member_id`가 필수이므로 카카오 로그인 성공 후 회원과 연결한다.

### 2. 로컬 로그인 정보 확인

| 로컬 상태 | 다음 처리 |
|---|---|
| Access·Refresh Token 모두 없음 | 로그인 |
| Access Token 있음 | 세션 확인 |
| Access Token 만료 + Refresh Token 있음 | 재발급 후 세션 확인 |
| Refresh Token 없음·만료·유효하지 않음 | 로컬 인증정보 삭제 후 로그인 |

토큰이 없다는 것은 앱 시작 단계의 정상 상태이며 HTTP 오류가 아니다.

### 3. 서버 세션 확인

```http
GET /api/v1/auth/session
Authorization: Bearer <access-token>
```

서버는 다음 순서로 검증한다.

```text
JWT 서명·iss·aud·exp
→ JWT sid로 auth_sessions 조회
→ revoked_at 확인
→ expires_at 확인
→ sub와 auth_sessions.member_id 일치 확인
→ 연결 기기 확인
→ members.status 확인
→ onboarding_completed_at 확인
```

최소 JWT claim:

```text
sub: members.id
sid: auth_sessions.id
iss
aud
iat
exp
```

JWT에는 이메일, 닉네임, 카카오 사용자 ID를 넣지 않는다.

### 4. 회원·온보딩 분기

| 회원 상태 | 온보딩 | 결과 |
|---|---|---|
| `ACTIVE` | 미완료 | `COMPLETE_ONBOARDING` |
| `ACTIVE` | 완료 | `ENTER_APP` |
| `LOCKED` | 무관 | `ACCOUNT_LOCKED` 오류 |
| `PENDING_DELETION` | 무관 | `ACCOUNT_PENDING_DELETION` 오류 |

세션 확인 성공:

```json
{
  "data": {
    "authenticated": true,
    "session": {
      "id": "a4d01048-b488-491d-9b30-c16aef48bdf5",
      "expiresAt": "2026-08-06T09:30:15.123Z"
    },
    "account": {
      "memberId": "4d8b1818-38dc-4ee8-9b14-b8676f353e06",
      "displayName": "지연",
      "status": "ACTIVE",
      "onboardingCompletedAt": null,
      "version": 0
    },
    "nextAction": "COMPLETE_ONBOARDING"
  }
}
```

`account`는 카카오 로그인·온보딩 완료 응답과 같은 `AccountSummary` 구조를 그대로 재사용한다(`timezone`/`locale`은 포함하지 않는다).

`version`은 `members.version`(낙관적 잠금)이다. 회원 정보를 직접 수정하는 API를 호출할 때 그대로 함께 전달한다. 자세한 규칙은 [conventions.md](conventions.md#낙관적-잠금버전-충돌) 참고.

`nextAction`:

```text
COMPLETE_ONBOARDING
ENTER_APP
```

삭제 대기와 잠금은 성공 `nextAction`이 아니라 명시적 오류로 반환한다.

## 카카오 로그인

```http
POST /api/v1/auth/kakao/login
```

요청:

```json
{
  "providerAccessToken": "<kakao-provider-token>",
  "device": {
    "installationKey": "7d637e26-851d-4f13-83bf-cd296aa20d61",
    "platform": "IOS",
    "appVersion": "1.0.0"
  }
}
```

처리 순서:

```text
기기 DTO 검증
→ 카카오 토큰 검증 및 사용자 조회
→ KAKAO + provider_user_id로 SocialIdentity 조회
→ 없으면 Member와 SocialIdentity 생성
→ identity_status 확인
→ member_status 확인
→ DeviceInstallation 등록·갱신
→ AuthSession 생성
→ Access·Refresh Token 발급
→ 온보딩 완료 여부에 따른 nextAction 반환
```

로그인 성공: 신규 Member 생성 여부와 무관하게 항상 `200 OK`로 응답한다(로그인은 인증 행위이지 리소스 생성 행위가 아니며, 앱이 상태 코드로 신규/기존을 분기할 필요가 없게 한다).

```json
{
  "data": {
    "tokenType": "Bearer",
    "accessToken": "<access-token>",
    "accessTokenExpiresAt": "2026-07-23T10:30:15.123Z",
    "refreshToken": "<refresh-token>",
    "refreshTokenExpiresAt": "2026-08-06T09:30:15.123Z",
    "session": {
      "id": "a4d01048-b488-491d-9b30-c16aef48bdf5",
      "expiresAt": "2026-08-06T09:30:15.123Z"
    },
    "account": {
      "memberId": "4d8b1818-38dc-4ee8-9b14-b8676f353e06",
      "displayName": "지연",
      "status": "ACTIVE",
      "onboardingCompletedAt": null,
      "version": 0
    },
    "nextAction": "COMPLETE_ONBOARDING"
  }
}
```

- 카카오 토큰 원문은 저장하거나 로그에 남기지 않는다.
- 동일 이메일만으로 기존 회원과 자동 병합하지 않는다.

## Google 로그인

```http
POST /api/v1/auth/google/login
```

요청:

```json
{
  "idToken": "<google-id-token>",
  "device": {
    "installationKey": "7d637e26-851d-4f13-83bf-cd296aa20d61",
    "platform": "IOS",
    "appVersion": "1.0.0"
  }
}
```

처리 순서:

```text
기기 DTO 검증
→ Google ID Token 서명·issuer·audience·만료 검증(JWKS, RS256)
→ GOOGLE + sub로 SocialIdentity 조회
→ 없으면 Member와 SocialIdentity 생성
→ identity_status 확인
→ member_status 확인
→ DeviceInstallation 등록·갱신
→ AuthSession 생성
→ Access·Refresh Token 발급
→ 온보딩 완료 여부에 따른 nextAction 반환
```

응답은 카카오 로그인과 동일한 형태(`SocialLoginResponse`)를 그대로 재사용한다. `tokenType`/`accessToken`/`session`/`account`/`nextAction` 필드는 위 카카오 로그인 응답 예시와 완전히 같다.

- `sub`(Google 고유 사용자 ID)를 유일한 provider 식별자로 쓴다. Google Access Token은 회원 식별 근거로 쓰지 않는다.
- 허용 audience(`aud`)는 `GOOGLE_OAUTH_IOS_CLIENT_ID`/`GOOGLE_OAUTH_ANDROID_CLIENT_ID`/`GOOGLE_OAUTH_WEB_CLIENT_ID`로 서버에 등록된 값만 통과한다(플랫폼별 분기 없이 셋 다 같은 허용 목록으로 취급).
- ID Token 원문은 저장하거나 로그에 남기지 않는다.
- 동일 이메일만으로 기존(카카오 등) 회원과 자동 병합하지 않는다 - Google로 처음 로그인하면 이메일이 같아도 새 Member를 만든다.
- 검증 실패 시 `AUTH_PROVIDER_TOKEN_INVALID`(서명·issuer·audience·만료), `AUTH_PROVIDER_UNAVAILABLE`(JWKS 조회 등 외부 장애)로 응답한다. 이 두 코드는 provider 중립적이라 이후 Apple 로그인에도 그대로 재사용한다.
- 삭제 대기 복구는 `POST /api/v1/auth/google/restore`(아래 "삭제 대기 계정" 절 참고)를 쓴다.

## Apple 로그인

```http
POST /api/v1/auth/apple/login
```

요청:

```json
{
  "identityToken": "<apple-identity-token>",
  "rawNonce": "<client가 authorization 요청에 쓴 원문 nonce>",
  "fullName": "지연",
  "device": {
    "installationKey": "7d637e26-851d-4f13-83bf-cd296aa20d61",
    "platform": "IOS",
    "appVersion": "1.0.0"
  }
}
```

`fullName`은 Apple이 최초 승인 응답에서만 내려주는 값이라 클라이언트가 그 시점에만 채워 보낼 수 있다(선택값, 재로그인 시에는 비어 있어도 된다).

처리 순서:

```text
기기 DTO 검증
→ Apple identity token 서명·issuer·audience·만료 검증(JWKS, RS256)
→ rawNonce의 SHA-256 해시가 identity token의 nonce claim과 일치하는지 확인
→ APPLE + sub로 SocialIdentity 조회
→ 없으면 Member와 SocialIdentity 생성(표시 이름은 요청의 fullName, 없으면 기본값)
→ identity_status 확인
→ member_status 확인
→ DeviceInstallation 등록·갱신
→ AuthSession 생성
→ Access·Refresh Token 발급
→ 온보딩 완료 여부에 따른 nextAction 반환
```

응답은 카카오·Google 로그인과 동일한 `SocialLoginResponse`를 그대로 재사용한다.

- `sub`(Apple 고유 사용자 ID)를 유일한 provider 식별자로 쓴다.
- nonce 검증은 필수다. 클라이언트는 원문 nonce를 생성해 그 SHA-256 해시를 Apple authorization 요청에 실어 보내고(Apple 공식 가이드), 로그인 요청에는 원문 `rawNonce`를 그대로 담아 보낸다. 서버는 같은 해시를 계산해 identity token의 `nonce` claim과 비교한다 - 불일치·누락 시 `AUTH_PROVIDER_TOKEN_INVALID`.
- 허용 audience(`aud`)는 `APPLE_OAUTH_CLIENT_ID`(iOS 앱 Bundle ID)로 서버에 등록된 값만 통과한다.
- Apple이 내려주는 이메일이 비공개 릴레이 주소(`@privaterelay.appleid.com`)여도 일반 이메일과 동일하게 저장·처리한다.
- identity token 원문은 저장하거나 로그에 남기지 않는다.
- 동일 이메일만으로 기존 회원과 자동 병합하지 않는다.
- 검증 실패 시 Google과 동일한 `AUTH_PROVIDER_TOKEN_INVALID`/`AUTH_PROVIDER_UNAVAILABLE`로 응답한다.
- Apple 서버-투-서버 자격증명 철회 알림(webhook) 수신은 별도 작업으로 남아 있다.
- 삭제 대기 복구는 `POST /api/v1/auth/apple/restore`(아래 "삭제 대기 계정" 절 참고)를 쓴다.

## 토큰 재발급

```http
POST /api/v1/auth/refresh
```

요청:

```json
{
  "refreshToken": "<refresh-token>",
  "installationKey": "7d637e26-851d-4f13-83bf-cd296aa20d61"
}
```

처리 순서:

```text
Refresh Token에서 세션 식별
→ auth_sessions 조회
→ revoked_at·expires_at 확인
→ device_installation_id와 installationKey 확인
→ 저장된 refresh_token_hash와 비교
→ members.status 재확인 (LOCKED·PENDING_DELETION이면 재발급하지 않음)
→ 새 Refresh Token으로 회전
→ refresh_token_hash·last_used_at 갱신
→ 새 Access·Refresh Token 반환
```

재발급 시점에도 회원 상태를 다시 확인한다. Access Token 발급 이후 회원이 `LOCKED`되거나 탈퇴 요청을 했다면, 기존 Access Token은 TTL 만료 전까지는 유효하더라도 재발급은 로그인과 동일하게 `ACCOUNT_LOCKED`/`ACCOUNT_PENDING_DELETION`으로 응답한다.

현재 ERD에서는 이전 Refresh Token의 재사용 여부를 별도 판별하지 않는다. 해시가 일치하지 않으면 `AUTH_REFRESH_TOKEN_INVALID`로 응답하고 세션을 폐기한다.

## 온보딩 완료

```http
POST /api/v1/account/onboarding/complete
Authorization: Bearer <access-token>
```

요청: `/auth/session` 또는 로그인 응답에서 받은 `account.version`을 그대로 포함한다.

```json
{
  "version": 0,
  "displayName": "지연",
  "timezone": "Asia/Seoul",
  "locale": "ko-KR",
  "consents": [
    { "type": "TERMS", "documentVersion": "1.0", "agreed": true },
    { "type": "PRIVACY", "documentVersion": "1.0", "agreed": true },
    { "type": "AI_PROCESSING", "documentVersion": "1.0", "agreed": true }
  ],
  "notificationPreferences": [
    { "type": "WEEKLY_REFLECTION", "enabled": true, "dayOfWeek": 1, "localTime": "09:00:00" }
  ]
}
```

처리 순서:

```text
ACTIVE 회원·유효 세션 확인
→ version 일치 확인 (불일치 시 ACCOUNT_VERSION_CONFLICT)
→ 현재 문서 버전 확인
→ TERMS·PRIVACY·AI_PROCESSING 필수 동의 저장
→ 닉네임·timezone·locale 갱신
→ 선택한 알림 설정 저장
→ 모든 처리가 성공한 뒤 onboarding_completed_at 기록, version 증가
```

`onboarding_completed_at`은 마지막에 기록한다. 중간 단계 실패 시 온보딩 완료로 표시하지 않는다. `version`이 요청값과 다르면 다른 요청(다른 기기 등)이 먼저 회원 정보를 바꾼 것이므로 [error-response.md](error-response.md#낙관적-잠금-충돌)의 `ACCOUNT_VERSION_CONFLICT`로 응답하고 저장하지 않는다.

응답:

```json
{
  "data": {
    "account": {
      "memberId": "4d8b1818-38dc-4ee8-9b14-b8676f353e06",
      "displayName": "지연",
      "status": "ACTIVE",
      "onboardingCompletedAt": "2026-07-23T09:30:15.123Z",
      "version": 1
    },
    "nextAction": "ENTER_APP"
  }
}
```

네트워크 재시도를 고려해 이미 완료된 동일 요청(같은 `version`으로 이미 완료된 상태)은 동의 이력을 중복 생성하지 않고 현재 완료 상태를 반환한다.

## 로그아웃

```http
POST /api/v1/auth/logout
Authorization: Bearer <access-token>
```

- 현재 `sid`의 `auth_sessions.revoked_at` 기록
- `revoke_reason = LOGOUT`
- 응답: `204 No Content`
- 앱은 응답 성공 여부와 무관하게 로컬 토큰과 민감 캐시를 제거한다.
- 위젯은 민감 내용을 비공개 상태로 전환한다.

## 삭제 대기 계정

탈퇴 요청 시:

```text
members.status = PENDING_DELETION
withdrawal_requested_at 기록
scheduled_deletion_at 기록
모든 auth_sessions 폐기
즉시 로그아웃
```

삭제 대기 회원이 일반 로그인(`/login`)을 하면 정상 세션을 자동 발급하지 않는다.

```text
provider 본인 확인
→ ACCOUNT_PENDING_DELETION
→ 삭제 예정일 표시
→ 사용자가 삭제 취소를 명시적으로 선택
→ provider 재인증 기반 복구
→ ACTIVE 전환 후 새 세션 발급
```

자동 복구는 금지한다. 복구 성공 시 `withdrawal_requested_at`과 `scheduled_deletion_at`을 함께 비우고 새 세션을 만든다.

각 provider마다 로그인과 분리된 전용 복구 엔드포인트가 있다. 요청 본문은 같은 provider의 로그인 요청과 완전히 같은 형태를 그대로 쓴다(별도의 `confirmRecovery` 필드는 없다 - `/login`과 분리된 경로 자체가 "명시적 복구 확인"이다).

| Provider | 경로 |
|---|---|
| Kakao | `POST /api/v1/auth/restore` |
| Google | `POST /api/v1/auth/google/restore` |
| Apple | `POST /api/v1/auth/apple/restore` |

서버는 provider 사용자가 기존 `social_identities`와 일치하는지 다시 확인한 후에만 복구한다. 성공 응답은 로그인 성공 응답과 같은 `SocialLoginResponse` 구조를 그대로 쓴다.

이 엔드포인트들은 `version`을 요구하지 않는다. 로그인이 막힌 상태에서 호출하므로 클라이언트가 최신 `version`을 미리 알 수 없고, 복구는 필드를 덮어쓰는 수정이 아니라 상태 전환(provider 재인증으로 검증됨)이라 동시 호출이 발생해도 데이터 손실 위험이 없다.

PENDING_DELETION 상태는 로그아웃 처리로 모든 세션이 이미 폐기되어 있어 이 엔드포인트들은 Access Token 없이 호출할 수 있어야 한다(`SecurityConfig` 공개 경로).

## 요청 제한(Rate Limit)

다음 공개 엔드포인트는 남용·무차별 대입 방지를 위해 요청 빈도를 제한한다. 초과 시 [error-response.md](error-response.md#공통-오류)의 `COMMON_RATE_LIMITED`(`429`)로 응답하고 `Retry-After` 헤더를 포함한다.

| 엔드포인트 | 권장 제한 | 기준 |
|---|---|---|
| `POST /api/v1/auth/kakao/login` | 분당 10회 | 요청 IP |
| `POST /api/v1/auth/google/login` | 분당 10회 | 요청 IP |
| `POST /api/v1/auth/apple/login` | 분당 10회 | 요청 IP |
| `POST /api/v1/auth/refresh` | 분당 20회 | `installationKey` (기준 식별자를 아직 못 구했다면 IP로 보조 제한) |
| `POST /api/v1/auth/restore` | 분당 5회 | 요청 IP |
| `POST /api/v1/auth/google/restore` | 분당 5회 | 요청 IP |
| `POST /api/v1/auth/apple/restore` | 분당 5회 | 요청 IP |

구체적인 수치는 실제 트래픽을 보고 조정 가능한 권장값이며, 이번 계약에서 확정하는 것은 "위 엔드포인트들에는 반드시 제한이 있어야 한다"는 원칙이다.

## 공개·보호 엔드포인트

| 엔드포인트 | 접근 |
|---|---|
| `/api/v1/health` | 공개 |
| `/api/v1/auth/kakao/login` | 공개, 카카오 토큰 검증 필수 |
| `/api/v1/auth/restore` | 공개, 카카오 재인증과 명시적 복구 확인 필수 |
| `/api/v1/auth/google/login` | 공개, Google ID Token 검증 필수 |
| `/api/v1/auth/google/restore` | 공개, Google 재인증과 명시적 복구 확인 필수 |
| `/api/v1/auth/apple/login` | 공개, Apple identity token 검증 필수 |
| `/api/v1/auth/apple/restore` | 공개, Apple 재인증과 명시적 복구 확인 필수 |
| `/api/v1/auth/refresh` | 공개, Refresh Token 검증 필수 |
| `/api/v1/auth/session` | Access Token 필요 |
| `/api/v1/auth/logout` | Access Token 필요 |
| `/api/v1/account/onboarding/complete` | Access Token 필요 |

“공개”는 무검증을 의미하지 않는다. Spring Security Access Token이 필요하지 않을 뿐 각 엔드포인트의 자격정보는 반드시 검증한다.
