# Architecture 개요

## 기술 스택

- Java 21
- Spring Boot 4.1.0
- Gradle 9.5.1 (Gradle Kotlin DSL)
- Spring Web MVC
- Spring Data JPA / Hibernate ORM
- Jakarta Bean Validation
- Spring Boot Actuator
- PostgreSQL (Supabase Managed PostgreSQL)
- Flyway Database Migration
- Springdoc OpenAPI (webmvc-ui) + Swagger UI
- JUnit 5 / Spring Boot Test

Spring Security, Kakao 연동, Redis 등 위 목록에 없는 의존성은 아직 도입되지 않았습니다. 실제 설치 여부는 `build.gradle.kts`를 기준으로 확인합니다.

## 패키지 구조

- 기본 패키지: `com.naroom.api`
- 기능 중심 패키지 구조를 따르며, 여러 기능에서 공유하는 관심사만 `com.naroom.api.global`에 둡니다.
- 비즈니스 API는 `/api/v1` 하위에 위치합니다. Actuator 등 운영 관리 엔드포인트는 예외입니다.

## 데이터베이스

- Flyway가 스키마 이력을 소유합니다 (`src/main/resources/db/migration`).
- `spring.jpa.hibernate.ddl-auto=validate` — Hibernate가 스키마를 생성하지 않고 Flyway 결과만 검증합니다.

## API 문서

- Swagger UI / OpenAPI 엔드포인트는 `springdoc-openapi-starter-webmvc-ui`가 제공합니다.
- 공식 정적 계약 파일은 [`docs/api/openapi.yaml`](../api/openapi.yaml)이며, 재현 가능한 Gradle task(`generateOpenApiDocs`)로 생성합니다.
- 상세 규칙은 [API 소개](../api/index.md)와 [API 문서 자동화 가이드](../guides/api-documentation.md)를 참고합니다.

## 상태

- 상태: 현재 저장소 구성 기준 작성됨 (2026-07-23)
- 인증, 공통 응답/오류 구조, Account Entity/Repository 등은 아직 구현 전이며, 관련 architecture 결정은 확정되는 시점에 ADR로 기록합니다.
