# CLAUDE.md

```
Claude Code
    └── CLAUDE.md
        └── @AGENTS.md
            ├── Expo 공식 규칙
            ├── 프론트 기술·작업 규칙
            └── docs/PRODUCT_CONTEXT.md 참조
```

## 프로젝트 개요

- 프로젝트명: `naroom-api`
- 역할: Naroom 모바일 앱을 위한 백엔드 API
- 기본 패키지: `com.naroom.api`
- 제품 목적과 사용자 맥락은 `docs/PRODUCT_CONTEXT.md`를 먼저 확인한다.
- 현재 작업 범위와 완료 기준은 해당 GitHub Issue 또는 사용자가 제공한 요구사항을 기준으로 한다.

## 기술 스택

- Java 21
- Spring Boot 4.1.0
- Gradle Kotlin DSL
- Gradle Wrapper
- Spring Web
- Bean Validation
- Spring Boot Actuator
- Spring Boot DevTools
- JUnit 및 Spring Boot Test

현재 프로젝트에 아직 도입되지 않은 데이터베이스, Redis, 인증, 외부 API 의존성은 요구사항 없이 임의로 추가하지 않는다.

## 주요 경로

- 애플리케이션 코드: `src/main/java/com/naroom/api`
- 테스트 코드: `src/test/java/com/naroom/api`
- 환경설정: `src/main/resources`
- 공통 제품 맥락: `docs/PRODUCT_CONTEXT.md`
- 앱 상태 확인 API: `GET /api/v1/health`
- Actuator 상태 확인: `GET /actuator/health`

## 로컬 실행

```bash
./gradlew bootRun
```

기본 로컬 프로필은 `local`이다.

애플리케이션은 `.env.local`을 선택적으로 불러올 수 있지만, Claude Code는 해당 파일의 내용을 읽거나 출력해서는 안 된다.

## 빌드 및 검증

```bash
./gradlew test
./gradlew clean build
```

코드를 변경한 후에는 다음을 수행한다.

1. 변경 범위에 해당하는 테스트를 실행한다.
2. `./gradlew clean build`를 실행한다.
3. 변경한 파일과 검증 결과를 사용자에게 보고한다.
4. 검증하지 못한 항목이 있다면 이유를 명시한다.

## 코딩 규칙

- 비즈니스 API는 `/api/v1` 하위에 작성한다.
- Actuator 같은 운영 관리 엔드포인트는 위 규칙의 예외다.
- 기본 패키지 `com.naroom.api` 하위에서 기능 중심 패키지 구조를 따른다.
- 클래스, 메서드, 변수 이름은 역할이 드러나는 영어 이름을 사용한다.
- 의존성 주입은 생성자 주입을 우선한다.
- 요청값 검증은 Bean Validation을 우선 사용한다.
- Controller에 비즈니스 로직을 과도하게 작성하지 않는다.
- 단순 응답 모델은 Java record를 사용할 수 있다.
- API 요청·응답 계약을 변경할 경우 변경 영향과 이유를 먼저 설명한다.
- 새로운 의존성, 인프라 연동, DB 스키마, 인증 구조는 요구사항 없이 임의로 추가하지 않는다.
- 요청 범위와 관계없는 리팩터링이나 파일 정리는 수행하지 않는다.
- 기존 코드 스타일과 이미 결정된 프로젝트 구조를 따른다.

## 환경변수 규칙

환경변수 이름과 안전한 예시는 `.env.example`에서 관리한다.

실제 비밀값은 다음 위치 중 어느 곳에도 작성하지 않는다.

- 소스 코드
- 테스트 코드
- 로그
- README 및 프로젝트 문서
- `.env.example`
- Git 커밋
- Claude 대화 내용

## 민감정보 및 접근 금지 대상

Claude Code는 다음 파일과 값을 읽거나 수정하거나 출력해서는 안 된다.

- `.env.local`
- `.env.*.local`
- DB 및 Redis 비밀번호
- JWT 서명 키
- OpenAI API 키
- Firebase 및 Google Cloud 서비스 계정 JSON
- 인증서 및 키 파일
- keystore
- 외부 서비스 토큰
- 실제 사용자 데이터와 인증 토큰

민감정보가 이미 코드나 Git 변경사항에 포함된 것을 발견하면 값을 출력하지 말고, 노출 가능성만 사용자에게 알린다.

## 수정 금지 또는 주의 대상

명시적인 요청 없이 다음을 수정하지 않는다.

- `build/`
- `.gradle/`
- `.idea/`
- `.env.local`
- `gradle/wrapper/gradle-wrapper.jar`
- Gradle Wrapper 버전과 구성
- `.gitignore`의 민감정보 제외 규칙

Gradle Wrapper를 변경해야 한다면 파일을 직접 편집하지 말고, 변경 이유와 공식 Wrapper 갱신 명령을 먼저 제안한다.

## 작업 절차

1. `docs/PRODUCT_CONTEXT.md`를 확인한다.
2. 현재 GitHub Issue 또는 사용자가 제공한 요구사항을 확인한다.
3. 구현 범위와 제외 범위를 구분한다.
4. 수정 예정 파일과 검증 방법을 먼저 설명한다.
5. 승인된 범위 안에서만 작업한다.
6. 테스트와 빌드를 실행한다.
7. 변경사항, 검증 결과, 남은 위험 요소를 요약한다.

제품 문서와 현재 Issue가 충돌하면 임의로 판단하지 말고 사용자에게 확인한다.