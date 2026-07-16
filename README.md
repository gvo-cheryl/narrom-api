# NAROOM API

나로움 서비스의 백엔드 API입니다.

나로움은 감정과 일상을 기록하고, 쌓인 기록을 바탕으로 자신을 이해할 수 있도록 돕는 서비스입니다. 하루 단위의 기록과 AI 회고뿐 아니라 LifeTime을 통해 주간·월간·장기 흐름을 돌아보고, 반복되는 감정과 행동 패턴, 작은 변화를 확인하는 것을 목표로 합니다.

이 저장소에서는 사용자, 기록, AI 회고, LifeTime 분석, 챌린지와 성취 데이터를 관리합니다.

## 주요 기능

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

## Tech Stack

- Java
- Spring Boot
- Gradle
- Spring Web
- Spring Security
- Spring Data JPA
- QueryDSL
- PostgreSQL
- Redis
- Docker
- Google Cloud Run

AI 모델과 외부 서비스는 특정 공급자에 강하게 결합하지 않도록 별도의 연동 계층을 통해 관리합니다.

정확한 라이브러리 버전은 `build.gradle`을 기준으로 합니다.

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── io/naroom/
│   │       ├── domain/
│   │       ├── application/
│   │       ├── infrastructure/
│   │       ├── presentation/
│   │       └── common/
│   └── resources/
└── test/