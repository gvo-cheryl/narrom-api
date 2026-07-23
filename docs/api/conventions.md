# API 공통 규칙

상태: Beta 1 계약안

## 기본 규칙

- 기본 경로: `/api/v1`
- JSON 필드명: `camelCase`
- ID: UUID 문자열
- 성공 응답 Content-Type: `application/json`
- 오류 응답 Content-Type: `application/problem+json`
- Entity 직접 반환 금지
- 요청 DTO와 응답 DTO 분리

## 성공 응답

단건:

```json
{
  "data": {
    "id": "4d8b1818-38dc-4ee8-9b14-b8676f353e06"
  }
}
```

목록:

```json
{
  "data": []
}
```

커서 목록:

```json
{
  "data": [],
  "page": {
    "nextCursor": null,
    "hasNext": false
  }
}
```

성공 응답에는 `success`, 공통 성공 `code`, 공통 `message`, `timestamp`를 반복해서 넣지 않는다.

### 커서 페이지네이션 규칙

- 목록 조회 query parameter: `cursor`(선택, 없으면 첫 페이지), `size`(선택, 기본값·최대값은 API마다 정의하되 기본 20 / 최대 100을 권장 기준으로 삼는다)
- `nextCursor`는 클라이언트가 해석하지 않는 **opaque 문자열**로 취급한다. 정렬 키를 그대로 노출하지 않고 서버가 인코딩한 값을 그대로 다음 요청에 전달한다.
- `hasNext: false`이면 `nextCursor`는 `null`이다.
- offset 기반 페이지네이션은 사용하지 않는다.

## HTTP 상태

| 상황 | 상태 |
|---|---:|
| 조회·수정 성공 | `200 OK` |
| 생성 성공 | `201 Created` |
| 본문 없는 로그아웃·삭제 성공 | `204 No Content` |
| 입력 형식 오류 | `400 Bad Request` |
| 인증 실패 | `401 Unauthorized` |
| 인증됐지만 접근 불가 | `403 Forbidden` |
| 없거나 소유하지 않은 리소스 | `404 Not Found` |
| 현재 상태와 충돌 | `409 Conflict` |
| 요청 제한 | `429 Too Many Requests` |
| 외부 서비스 일시 장애 | `503 Service Unavailable` |

`204` 응답에는 `{ "data": null }`을 넣지 않는다.

`429`, `503`은 가능한 경우 `Retry-After` 헤더(초 단위)를 함께 반환한다.

## 응답 헤더

모든 응답(성공·오류 공통)에 `X-Trace-Id` 헤더를 포함한다.

- 성공 응답 본문에는 `traceId`를 넣지 않고 헤더로만 노출한다(본문 스키마를 오염시키지 않기 위함).
- 오류 응답은 헤더의 `X-Trace-Id`와 본문의 `traceId`가 항상 같은 값이어야 한다.
- 목적: 오류가 아니지만 느리거나 이상 동작한 요청도 헤더 값으로 서버 로그와 대조할 수 있게 한다.

## null·빈 값

- 목록에 값이 없으면 `[]`
- 값이 없으면 빈 문자열이 아니라 `null`
- 적용되지 않는 선택 필드는 생략할 수 있다.
- `onboardingCompletedAt == null`은 온보딩 미완료를 의미한다.
- `scheduledDeletionAt`은 `members.status == PENDING_DELETION`일 때만 값이 있어야 한다.

## 날짜·시간

| 의미 | Java 권장 타입 | API 표현 |
|---|---|---|
| 발생한 정확한 시각 | `Instant` | UTC ISO-8601 |
| 사용자 생활 날짜 | `LocalDate` | `YYYY-MM-DD` |
| 알림 시각 | `LocalTime` | `HH:mm:ss` |
| 회원 시간대 | `ZoneId` | IANA 이름 |

```json
{
  "createdAt": "2026-07-23T09:30:15.123Z",
  "recordDate": "2026-07-23",
  "timezone": "Asia/Seoul"
}
```

- DB의 `timestamptz`와 API의 정확한 시각은 UTC로 처리한다.
- “오늘”은 서버 시간대가 아니라 `members.timezone` 기준이다.
- API 경계에서 시간대가 없는 `LocalDateTime`을 사용하지 않는다.

## 인증 헤더

```http
Authorization: Bearer <access-token>
```

- Access Token만 `Authorization` 헤더로 전달한다.
- Refresh Token과 카카오 Provider Token은 요청 본문으로 전달하고 반드시 로그에서 마스킹한다.
- 인증 실패 `401`에는 가능한 경우 `WWW-Authenticate: Bearer`를 포함한다.

## 기기 정보

기기 정보는 카카오 로그인과 토큰 재발급 요청의 명시적 DTO로 전달한다.

```json
{
  "installationKey": "7d637e26-851d-4f13-83bf-cd296aa20d61",
  "platform": "IOS",
  "appVersion": "1.0.0"
}
```

- `installationKey`는 앱 설치 단위의 난수 식별자다.
- 익명 상태에서는 서버 `device_installations`에 저장하지 않는다.
- 로그인 성공 후 `members.id`와 연결해 등록하거나 갱신한다.
- `platform`은 현재 ERD의 `varchar(30)`과 맞춰 문자열로 처리한다.
- Push Token은 로그인 응답에 포함하지 않고 별도 인증 API에서 암호화해 저장한다.

## 낙관적 잠금(버전 충돌)

`members.version`처럼 낙관적 잠금 컬럼이 있는 리소스를 클라이언트가 직접 수정하는 API는 `ETag` 대신 요청·응답 DTO에 `version` 필드를 명시한다(모바일 앱과 OpenAPI 타입 생성 관점에서 더 단순함).

조회 응답:

```json
{
  "data": {
    "id": "4d8b1818-38dc-4ee8-9b14-b8676f353e06",
    "displayName": "지연",
    "timezone": "Asia/Seoul",
    "version": 3
  }
}
```

수정 요청은 조회 시 받은 `version`을 그대로 포함한다:

```json
{
  "displayName": "새 닉네임",
  "version": 3
}
```

수정 성공 응답은 증가한 `version`을 반환한다:

```json
{
  "data": {
    "id": "4d8b1818-38dc-4ee8-9b14-b8676f353e06",
    "displayName": "새 닉네임",
    "version": 4
  }
}
```

다른 요청이 먼저 수정했다면 `409 ACCOUNT_VERSION_CONFLICT`로 응답한다. 상세 오류 형식은 [error-response.md](error-response.md#낙관적-잠금-충돌)를 따른다.

규칙:

- 자동 재시도를 하지 않는다. 최신 데이터를 다시 조회하지 않고 재시도하면 다른 변경을 덮어쓸 수 있다.
- 클라이언트는 `ACCOUNT_VERSION_CONFLICT`를 받으면 리소스를 다시 조회한 뒤 사용자에게 재입력을 요청한다(`action: RELOAD_RESOURCE`).
- 적용 대상: 프로필 수정, timezone·locale 등 회원 설정 수정, 온보딩 완료 시 회원 정보 수정, 탈퇴 철회 등 회원 상태 변경.
- 적용하지 않는 대상: 로그인 확인, 세션 조회 등 회원 정보를 수정하지 않는 API.
- 구현에서 `ObjectOptimisticLockingFailureException`/`OptimisticLockException`만 `ACCOUNT_VERSION_CONFLICT`로 변환한다. 다른 DB 예외까지 낙관적 잠금 충돌로 뭉뚱그리지 않는다(자세한 예외 변환 규칙은 [exception-handling.md](exception-handling.md) 참고).

## 민감정보

다음 값은 응답, 오류, 일반 로그, OpenAPI 예시에 포함하지 않는다.

- Access Token, Refresh Token의 실제 값
- 카카오 Provider Token과 원본 응답
- `refresh_token_hash`
- `push_token_ciphertext`
- 실제 이메일과 `provider_user_id`
- 기록·감정·AI 요청 원문

