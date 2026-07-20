# CLAUDE.md

## 프로젝트 개요
- 프로젝트명: naroom-api
- 언어/버전: Java 21
- 프레임워크: Spring Boot 4.1.0
- 빌드 도구: Gradle (Kotlin DSL)
- 기본 패키지: `com.naroom.api`

## 로컬 실행
```
./gradlew bootRun
```

## 검증
코드 변경 후에는 관련 테스트와 아래 명령을 반드시 실행할 것.
```
./gradlew clean build
```

## API
- 기본 상태 API: `GET /api/v1/health`
- Actuator 헬스체크: `GET /actuator/health`
- 신규 API는 `/api/v1` 경로 하위에 작성할 것

## 금지 사항
- `.env.local`, 키 파일, 인증서, 서비스 계정 JSON은 읽기·수정·출력 금지
- `build/`, `.gradle/`, `.idea/`, Gradle Wrapper JAR은 직접 수정하지 말 것

## .env.example 관리
- 변수명과 안전한 예시 값만 관리
- 실제 비밀값(시크릿, 키, 토큰 등)은 절대 넣지 말 것

## 작업 원칙
- 기존 코드 스타일을 따를 것
- 작업 범위를 벗어나는 파일은 수정하지 말고, 먼저 사용자에게 설명할 것
