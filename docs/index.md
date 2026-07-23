# Naroom API Documentation

Naroom 백엔드(`naroom-api`)의 개발 문서 사이트입니다.

## 프로젝트 개요

- 제품 맥락: [PRODUCT_CONTEXT.md](PRODUCT_CONTEXT.md)
- Architecture 개요: [architecture/overview.md](architecture/overview.md)
- Account 도메인: [domain/account.md](domain/account.md)

## API 계약

- 공식 정적 계약 파일: [`docs/api/openapi.yaml`](api/openapi.yaml)
- API 문서 소개: [api/index.md](api/index.md)
- Swagger UI (local): `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

## 계약 재생성

```bash
./gradlew generateOpenApiDocs
```

`docs/api/openapi.yaml`을 다시 생성합니다. 서버 코드가 변경되면 이 명령으로 계약을 재생성하고, 서버 코드와 같은 commit에 포함해야 합니다.

## 문서 빌드

```bash
python3 -m venv .venv-docs
source .venv-docs/bin/activate
python -m pip install -r requirements-docs.txt
mkdocs build --strict
mkdocs serve
```

## 문서 최신성 규칙과 자동화

자세한 내용은 [guides/api-documentation.md](guides/api-documentation.md)를 참고합니다.

- `dev`, `main` 직접 push에서 테스트·계약 최신성·`mkdocs build --strict`가 검증됩니다.
- GitHub Pages 배포는 `main` push 또는 수동 실행에서만 수행됩니다.
