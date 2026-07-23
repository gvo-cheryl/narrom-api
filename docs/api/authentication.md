# 인증 API 계약

상태: Draft
확정 예정: 오늘 8번 인증 API 계약 설계 문서 작성, 오늘 9번 Spring Security 기본 설정

이 문서는 아직 확정되지 않았습니다.

## 현재까지 확정된 것

- 인증 스킴 이름: `bearerAuth` (HTTP bearer, JWT 포맷) — OpenAPI 문서에 스킴만 등록되어 있으며, 아직 모든 endpoint에 전역 인증을 강제하지 않습니다.
- 이번 백엔드 구현은 카카오 로그인을 우선으로 합니다.

## 아직 결정되지 않은 것

- Access Token / Refresh Token 발급·검증 방식
- 재발급, 토큰 회전, 로그아웃 흐름
- 인증 실패 시 오류 표현
- 공개 endpoint와 인증 필요 endpoint의 범위
- Spring Security 설정 자체 (현재 저장소에 Spring Security 의존성이 없습니다)

확정 전까지 임의의 인증 흐름이나 엔드포인트를 작성하지 않습니다.
