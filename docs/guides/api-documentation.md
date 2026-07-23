# API 문서 자동화 가이드

## 1. 환경 준비

- Java 21, Gradle Wrapper(`./gradlew`)를 사용합니다.
- `spring.profiles.default: local`이 기본이며, `application-local.yml`은 PostgreSQL(DB_URL 등) 접속 정보를 필요로 합니다.
- 로컬 실행 시 `.env.local`이 있으면 `spring.config.import`로 자동 로드됩니다. 값을 직접 열람하거나 출력하지 않습니다.

## 2. OpenAPI 문서 생성

```bash
./gradlew generateOpenApiDocs
```

이 task는 다음을 수행합니다.

1. 컴파일된 애플리케이션을 임시 포트(`8099`)로 기동합니다.
2. 기동 로그에서 정상 시작 메시지를 확인한 뒤에만 다음 단계로 진행합니다 (고정 `sleep` 하나에만 의존하지 않음).
3. `/v3/api-docs.yaml`을 요청해 HTTP 200과 비어 있지 않은 본문, OpenAPI 3.1 여부를 확인합니다.
4. 결과를 `docs/api/openapi.yaml`에 저장합니다.
5. 기동한 프로세스를 종료합니다.

애플리케이션 기동이 실패하거나 응답이 비정상이면 task 자체가 실패합니다.

## 3. Endpoint 직접 확인 (선택)

```bash
./gradlew bootRun
```

```text
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
http://localhost:8080/v3/api-docs.yaml
```

## 4. 계약 검증

- 자동 검증: `./gradlew test` 실행 시 `OpenApiDocsTest`가 parser(Jackson YAMLMapper) 기반으로 `openapi` 버전, `info.title`, `info.version`, `components.securitySchemes.bearerAuth`, Health API 포함 여부, 민감정보 미포함 여부를 검증합니다.
- 최신성 검증: 커밋된 계약과 재생성 결과가 같은지 확인합니다.

```bash
./gradlew generateOpenApiDocs
git diff --exit-code -- docs/api/openapi.yaml
```

이 명령이 diff를 반환하면, 서버 코드 변경으로 계약이 바뀌었는데 재생성된 파일이 아직 커밋되지 않았다는 의미입니다.

## 5. MkDocs build와 serve

```bash
python3 -m venv .venv-docs
source .venv-docs/bin/activate
python -m pip install -r requirements-docs.txt
mkdocs build --strict
mkdocs serve
```

`mkdocs serve`는 기동과 페이지 응답을 확인한 뒤 종료합니다. background process를 남기지 않습니다.

## 6. 문서 변경 시 갱신 순서

1. Controller, request/response DTO, validation, API path, OpenAPI annotation을 변경합니다.
2. `./gradlew generateOpenApiDocs`로 `docs/api/openapi.yaml`을 재생성합니다.
3. 서버 코드와 재생성된 `docs/api/openapi.yaml`을 같은 commit에 포함합니다.
4. 필요하면 `docs/domain/*.md`, `docs/api/*.md` 등 관련 Markdown 문서를 갱신합니다.
5. `mkdocs build --strict`로 문서 빌드를 확인합니다.

## 7. 문제 해결

| 증상 | 확인할 것 |
| --- | --- |
| `generateOpenApiDocs`가 타임아웃으로 실패 | `build/openapi-docs/bootRun.log` 확인. 대부분 DB 접속 실패(`DB_URL` 등 환경변수 부재)가 원인입니다. |
| `./gradlew test`에서 `contextLoads()` 실패 | PostgreSQL 접속 정보(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)가 현재 셸/`.env.local`에 없는지 확인합니다. |
| `docs/api/openapi.yaml` diff 발생 | 서버 코드 변경 후 재생성을 누락했는지, 혹은 두 번 연속 생성 시에도 diff가 나는 비결정적 요인(포트, 타임스탬프 등)이 새로 생겼는지 확인합니다. |
| `mkdocs build --strict` 실패 | `mkdocs.yml`의 `nav`에 없는 파일이 있는지, 또는 깨진 내부 링크가 있는지 확인합니다. |
