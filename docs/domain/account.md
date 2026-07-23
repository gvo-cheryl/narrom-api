# Account 도메인

상태: Draft
확정 예정: 오늘 6번 Account 도메인·Entity·Repository 구현, 오늘 8번 인증 API 계약

이 문서는 아직 확정되지 않은 API나 Entity 상세를 임의로 채우지 않습니다. 아래는 현재까지 확정된 사실만 기록합니다.

## 확정된 데이터 설계 원칙

- 내부 회원 `Member`와 외부 식별자 `SocialIdentity`를 분리합니다.
- 데이터 구조는 향후 여러 소셜 제공자를 연결할 수 있도록 확장 가능하게 유지합니다.
- 이름, 생년월일, 성별, 또는 동일 이메일만으로 계정을 자동 통합하지 않습니다.
- 세션은 `auth_sessions`로 관리합니다.
- 탈퇴 요청 직후 접근을 차단하고, 7일 삭제 유예 후 물리 삭제할 수 있는 구조를 유지합니다.
- Entity를 API 응답으로 직접 노출하지 않으며, request DTO와 response DTO를 분리합니다.

## 현재 로그인 범위

- 이번 백엔드 구현은 카카오 로그인 우선입니다.
- Google 로그인은 현재 일정에 포함되지 않습니다.
- 카카오 로그인 구현 자체는 아직 시작되지 않았습니다.

## 적용된 Flyway 스키마 (V1)

`src/main/resources/db/migration/V1__create_account_schema.sql` 기준, Account 도메인 범위는 다음과 같습니다.

### Enum (5개)

- `member_status`
- `social_provider`
- `identity_status`
- `consent_type`
- `notification_type`

### Table (6개)

- `members`
- `social_identities`
- `device_installations`
- `auth_sessions`
- `member_consents`
- `notification_preferences`

## 아직 구현되지 않은 것

- Account 관련 JPA Entity, Repository
- 인증 API (카카오 로그인, 토큰 발급/재발급/로그아웃)
- Account 관련 Controller와 request/response DTO
