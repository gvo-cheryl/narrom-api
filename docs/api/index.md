# API 소개

## 공식 계약 파일

`naroom-api`의 공식 API 계약은 저장소에 커밋된 정적 파일 하나입니다.

```text
docs/api/openapi.yaml
```

사람이 이 파일을 직접 수정하지 않습니다. 서버 코드(Controller, DTO, validation)가 유일한 진실 공급원이며, 이 파일은 항상 서버 코드로부터 재생성합니다.

`naroom-app`(프론트엔드)의 TypeScript 타입 생성은 이 파일만 입력으로 사용합니다.

## Local 개발 환경

애플리케이션을 local profile로 실행하면 다음 경로를 사용할 수 있습니다.

| 항목 | 경로 |
| --- | --- |
| Swagger UI | `/swagger-ui/index.html` |
| OpenAPI JSON | `/v3/api-docs` |
| OpenAPI YAML | `/v3/api-docs.yaml` |

## 계약 재생성

```bash
./gradlew generateOpenApiDocs
```

애플리케이션을 안전하게 기동해 `/v3/api-docs.yaml`을 가져와 `docs/api/openapi.yaml`에 저장합니다. 같은 코드에서 반복 실행해도 의미 없는 diff가 발생하지 않습니다.

## 관련 문서

- [공통 규칙](conventions.md) — Draft
- [인증](authentication.md) — Draft
- [오류 응답](error-response.md) — Draft
- [API 문서 자동화 가이드](../guides/api-documentation.md)
