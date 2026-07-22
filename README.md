# NAROOM API

나로움 서비스의 백엔드 API입니다.

나로움은 감정과 일상을 기록하고, 쌓인 기록을 바탕으로 자신을 이해할 수 있도록 돕는 서비스입니다. 하루 단위의 기록과 AI 회고뿐 아니라 LifeTime을 통해 주간·월간·장기 흐름을 돌아보고, 반복되는 감정과 행동 패턴, 작은 변화를 확인하는 것을 목표로 합니다.

이 저장소에서는 사용자, 기록, AI 회고, LifeTime 분석, 챌린지와 성취 데이터를 관리합니다.

현재 버전은 Beta 1 모바일 애플리케이션을 위한 Spring Boot 기반 REST API 서버입니다.

## 1. 주요 기능

- 사용자 가입 및 인증
- 오늘의 감정·에너지 체크인
- 감사·감정·일상 기록
- AI 기반 기록 정리와 회고
- 주간·월간 LifeTime 회고
- 장기 감정·행동 패턴 분석
- 사용자 주도 자기정리
- 챌린지 및 뱃지 관리
- 알림 및 사용자 설정
- 기록 데이터 내보내기 및 삭제


## 2. Tech Stack

### Core

- Java 21
- Spring Boot 4.1.0
- Gradle 9.5.1
- Gradle Kotlin DSL

### Backend

- Spring Web MVC
- Spring Data JPA
- Hibernate ORM
- Jakarta Bean Validation

### Database

- PostgreSQL
- Supabase Managed PostgreSQL
- Flyway Database Migration

### Test

- JUnit 5
- Spring Boot Test
- Mockito

Spring Framework, Spring Data JPA, Hibernate, Flyway, JUnit 등 Spring Boot 하위 의존성은 특별한 이유가 없다면 개별 버전을 직접 지정하지 않고 Spring Boot 4.1.0의 의존성 관리 버전을 사용합니다.

AI 모델과 외부 서비스는 특정 공급자에 강하게 결합하지 않도록 별도의 연동 계층을 통해 관리합니다.

정확한 라이브러리 버전은 `build.gradle`을 기준으로 합니다.


## 3. Backend Architecture

Naroom API는 다음 구조를 기본 원칙으로 사용한다.

- 모바일 앱은 데이터베이스에 직접 접근하지 않는다
- 모든 데이터 접근은 Naroom Spring Boot API를 통해 수행한다.
- Supabase는 Beta 1에서 관리형 PostgreSQL로 사용한다.
- Supabase Auth, Storage, Realtime은 Beta 1 범위에서 사용하지 않는다.
- 코드는 기술 계층보다 도메인을 기준으로 분리한다.
- 각 도메인 내부에서는 얕은 계층 구조를 사용한다.
- 완전한 헥사고날 아키텍처나 Spring Modulith는 현재 도입하지 않도록 한다.

```text
Naroom App
    ↓ HTTPS / REST API
Naroom Spring Boot API
    ↓ JPA / JDBC
Supabase PostgreSQL
```

## 4. Package Structure

```text
com.naroom.api
├── NaroomApiApplication.java
│
├── global
│   ├── config
│   ├── error
│   ├── response
│   ├── security
│   ├── health
│   └── util
│
├── account
│   ├── controller
│   ├── dto
│   │   ├── request
│   │   └── response
│   ├── service
│   ├── domain
│   │   └── type
│   └── repository
│
├── checkin
├── entry
├── reflection
├── lifetime
├── experiment
├── content
└── notification
```

## 5. Project Directory Structure

```text
naroom-api
├── docs
│   ├── database
│   │   └── reference
│   └── development
│       ├── verification
│       └── PRODUCT_CONTEXT.md
│
├── gradle
│   └── wrapper
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── naroom
│   │   │           └── api
│   │   │               ├── NaroomApiApplication.java
│   │   │               │
│   │   │               ├── global
│   │   │               │   ├── config
│   │   │               │   ├── error
│   │   │               │   ├── response
│   │   │               │   ├── security
│   │   │               │   ├── health
│   │   │               │   └── util
│   │   │               │
│   │   │               └── account
│   │   │                   ├── controller
│   │   │                   ├── dto
│   │   │                   │   ├── request
│   │   │                   │   └── response
│   │   │                   ├── service
│   │   │                   ├── domain
│   │   │                   │   └── type
│   │   │                   └── repository
│   │   │
│   │   └── resources
│   │       ├── db
│   │       │   └── migration
│   │       │       └── V1__create_account_schema.sql
│   │       ├── application.yml
│   │       └── application-local.yml
│   │
│   └── test
│       └── java
│           └── com
│               └── naroom
│                   └── api
│                       ├── NaroomApiApplicationTests.java
│                       ├── global
│                       │   └── health
│                       └── account
│
├── .env.example
├── .env.local
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
└── README.md
```

## 6. Local Development

### 1. Environment file

프로젝트 루트에 `.env.local`을 생성합니다.

```dotenv
SPRING_PROFILES_ACTIVE=local
DB_URL=jdbc:postgresql://<host>:5432/<database>?sslmode=require
DB_USERNAME=<username>
DB_PASSWORD=<password>
```

### 2. Load environment variables

macOS `zsh` 터미널에서 다음 명령을 실행합니다.

```bash
set -a
source .env.local
set +a
```

이 설정은 현재 터미널 세션에만 적용됩니다.

### 3. Run tests

```bash
./gradlew clean test
```

### 4. Run application

```bash
./gradlew bootRun
```