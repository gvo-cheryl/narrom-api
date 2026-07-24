# Naroom API 계약

상태: Beta 1 계약안  
기준: Naroom Beta 1 IA 최종본, Naroom Beta 1 Data Dictionary, 현재 Account V1 ERD

## 문서 구성

| 문서 | 역할 |
|---|---|
| [conventions.md](conventions.md) | 성공 응답, 상태 코드, 날짜·시간, 페이지네이션, 낙관적 잠금, 필드 표현 |
| [error-response.md](error-response.md) | 공통 오류 응답, 단계별 오류 식별, 오류 코드 |
| [authentication.md](authentication.md) | 기기 확인, 카카오 로그인, 세션, 온보딩, 탈퇴 대기 흐름 |
| [content.md](content.md) | 오늘의 문장 조회·주제별 조회·저장 정책 |
| [exception-handling.md](exception-handling.md) | 서버 구현에서 예외를 `ProblemDetail`로 변환하는 구조 |
| `openapi.yaml` | 실제 엔드포인트와 요청·응답 스키마의 최종 계약 |

Markdown은 정책과 의미를 설명하고, 실제 필드 스키마는 `openapi.yaml`을 최종 기준으로 삼는다.

## 로컬 개발 환경과 계약 재생성

애플리케이션을 local profile로 실행하면 다음 경로를 사용할 수 있다.

| 항목 | 경로 |
| --- | --- |
| Swagger UI | `/swagger-ui/index.html` |
| OpenAPI JSON | `/v3/api-docs` |
| OpenAPI YAML | `/v3/api-docs.yaml` |

```bash
./gradlew generateOpenApiDocs
```

위 명령으로 `docs/api/openapi.yaml`을 재생성한다. 사람이 이 파일을 직접 수정하지 않는다. 자세한 절차와 트러블슈팅은 [API 문서 자동화 가이드](../guides/api-documentation.md)를 참고한다.

## Beta 1 시작 흐름

```text
앱 로컬 기기 정보 확인
→ 로컬 토큰 확인
→ 토큰 없음: 카카오 로그인
→ Access Token 만료: Refresh Token 재발급
→ 서버 세션·회원 상태 확인
→ 온보딩 미완료: 온보딩
→ ACTIVE + 온보딩 완료: 홈
→ PENDING_DELETION: 탈퇴 철회 확인
```

다음 네 상태를 반드시 구분한다.

| 상태 | 처리 |
|---|---|
| 로그인 정보 없음 | 로그인 화면 |
| 유효한 세션 + 온보딩 미완료 | 온보딩 |
| 유효한 세션 + `ACTIVE` + 온보딩 완료 | 홈 |
| `PENDING_DELETION` | 삭제 예정일 안내 후 철회 여부 확인 |

`LOCKED` 회원은 홈이나 온보딩으로 진입시키지 않는다.

## 최신 ERD 반영 범위

인증·시작 흐름은 다음 테이블을 기준으로 한다.

| 테이블 | 사용 목적 |
|---|---|
| `members` | 회원 상태, 시간대, 언어, 온보딩 완료, 탈퇴·삭제 예정 |
| `social_identities` | 카카오 등 외부 로그인 식별자 |
| `device_installations` | 회원과 연결된 앱 설치 기기 |
| `auth_sessions` | 현재 Refresh Token 해시와 세션 상태 |
| `member_consents` | 약관·개인정보·AI 처리 동의 이력 |
| `notification_preferences` | 온보딩 또는 설정에서 선택한 알림 |

## 구현 시 필수 동기화

1. API 변경 시 `openapi.yaml`과 관련 Markdown을 함께 수정한다.
2. Entity를 API 응답으로 직접 반환하지 않는다.
3. 오류는 모두 `application/problem+json`으로 통일한다.
4. Spring Security에서 발생한 인증·인가 오류도 동일한 오류 구조를 사용한다.
5. 실제 토큰, 카카오 사용자 ID, 이메일, 기록 원문을 문서 예시에 넣지 않는다.

