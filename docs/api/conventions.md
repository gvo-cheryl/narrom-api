# 공통 API 계약 규칙

상태: Draft
확정 예정: 오늘 3번 나로움 공통 API 계약 규칙 확정

이 문서는 아직 확정되지 않았습니다. 다음 항목은 오늘 3번 단계에서 결정합니다.

- 공통 성공 응답 구조
- 공통 오류 응답 구조와 오류 코드 체계
- 날짜·시간과 timezone 규칙
- HTTP status와 business error code의 역할 분리
- 단건·목록·pagination 응답 구조
- `traceId` 등 request correlation identifier 포함 여부
- validation error field 구조
- null과 빈 배열 표현 규칙

확정 전까지 임의의 JSON 스키마나 예시를 작성하지 않습니다.
