# 오늘의 문장(Content) 계약

상태: Beta 1 계약안
구현 범위: 오늘의 문장 조회, 주제별 조회, 저장·저장 취소, 저장 목록 조회

## ERD 기준

| 데이터 | 실제 기준 |
|---|---|
| 문장 | `quotes.id`, `text`, `author_name`, `source_name`, `source_url`, `status` |
| 문장 게시 상태 | `quotes.status`: `DRAFT`, `PUBLISHED`, `ARCHIVED` |
| 주제 | `quote_topics.id`, `code`, `name`, `active` |
| 문장-주제 연결 | `quote_topic_links`(다대다) |
| 저장 | `member_saved_quotes.member_id`, `quote_id`, `saved_at`(복합 PK) |

`status`가 `PUBLISHED`인 문장만 사용자에게 노출한다. `quote_topics.active`가 `false`인 주제는 `GET /api/v1/content/topics` 목록에서 제외한다.

## 오늘의 문장 배정 정책

별도 배정 테이블 없이, 게시 중인(`PUBLISHED`) 문장을 UTC 날짜(`epoch day`) 기준으로 전 사용자 공통 순환 배정한다. 배정 방식이 제품 요구사항과 다르면 서버 구현만 교체하면 되고 API 계약(요청·응답 스키마)은 바뀌지 않는다.

게시된 문장이 하나도 없으면 `CONTENT_QUOTE_NOT_FOUND`를 반환한다.

## 저장·저장 취소는 멱등이다

- 이미 저장된 문장을 다시 저장해도 에러 없이 성공 처리한다.
- 저장돼 있지 않은 문장의 저장을 취소해도 에러 없이 성공 처리한다.
- 두 요청 모두 본문 없는 성공 응답(HTTP 200, 빈 바디)이다.

## 응답 필드

`QuoteResponse.saved`는 요청 시점 인증된 회원 기준 저장 여부다. 인증 없는 요청은 이 API들에 접근할 수 없으므로 항상 회원 컨텍스트가 있다.

성공 응답은 [conventions.md](conventions.md)의 공통 규칙(`{"data": ...}` 래핑)을 그대로 따른다.

## 오류 코드

| 코드 | 상태 | 상황 |
|---|---|---|
| `CONTENT_QUOTE_NOT_FOUND` | 404 | 존재하지 않거나 삭제된 문장을 조회·저장하려 함, 또는 게시된 문장이 없음 |
| `CONTENT_QUOTE_TOPIC_NOT_FOUND` | 404 | 존재하지 않는 주제로 문장 목록을 조회하려 함 |

공통 오류 응답 형식은 [error-response.md](error-response.md)를 따른다.
