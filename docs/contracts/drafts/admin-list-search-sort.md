# 관리자 목록 API 검색·정렬 규정 (권장안 v1)

- 문서 상태: 권장안 v1 (초안)
- 대상: `naroom-api`의 `/api/v1/admin/**` 목록(GET) 엔드포인트, `naroom-admin`의 목록 화면
- Admin Web Implementation Spec §6.1(공통 목록 화면 기준)의 검색·정렬 항목을 실제 쿼리 계약으로 구체화한다.

## 1. 공통 쿼리 파라미터

모든 관리자 목록 GET 엔드포인트는 다음 두 파라미터를 선택적으로 지원한다.

| 파라미터 | 형식 | 설명 |
|---|---|---|
| `q` | 문자열 | 카테고리별로 정의된 검색 대상 필드에 대해 대소문자 무시 부분일치(`LIKE %q%`) OR 검색 |
| `sort` | `field,asc\|desc`(`;`로 이어 여러 개) | 방향 생략 시 `asc`. 카테고리별 허용 필드 목록에 없으면 `400 COMMON_VALIDATION_FAILED` |

`sort`에 `;`로 여러 `field,asc|desc`를 이어 붙이면 먼저 오는 필드가 우선순위 높은 다중 정렬이 된다(예: `status,asc;updatedAt,desc`). 두 파라미터 모두 생략하면 카테고리별 기본 정렬을 그대로 사용한다. 페이지네이션은 이번 규정에 포함하지 않는다 - 현재 데이터 규모에서는 검색으로 충분히 좁혀지고, 필요해지면 별도로 정의한다.

## 2. 카테고리별 규정

| 카테고리 | 엔드포인트 | 검색 대상 | 정렬 허용 필드 | 기본 정렬 |
|---|---|---|---|---|
| 오늘의 문장 | `GET /api/v1/admin/content/quotes` | text, authorName, sourceName, code | code, status, updatedAt, createdAt, activeFrom | code asc, versionNo desc |
| 기록 시작 질문 | `GET /api/v1/admin/content/record-prompts` | questionText, helperText, code | code, status, displayOrder, updatedAt, createdAt | code asc, versionNo desc |
| 앱 문구 | `GET /api/v1/admin/content/app-copy` | contentKey, valueText, surface | contentKey, surface, status, updatedAt, createdAt | contentKey asc, locale asc, versionNo desc |
| 작은 실험 주제 | `GET /api/v1/admin/experiments/topics` | name, code, description | name, code, displayOrder, updatedAt, createdAt | displayOrder asc |
| 작은 실험 미션 | `GET /api/v1/admin/experiments/missions` | title, code, description, instruction | code, title, missionType, updatedAt, createdAt | updatedAt desc |
| 작은 실험 코스 | `GET /api/v1/admin/experiments/programs` | title, code, description | code, title, status, updatedAt, createdAt | code asc, contentVersion desc |

`code`·`versionNo`(또는 `contentVersion`) 조합이 있는 카테고리는 "같은 code끼리 모아 최신 버전을 먼저 보여준다"는 기존 목록 화면 관성을 기본 정렬로 유지한다. `sort`를 명시하면 이 그룹핑 대신 지정한 단일 필드로 정렬한다.

## 3. 구현

- 검색: `com.naroom.api.admin.common.AdminSearchSpecifications.containsAnyIgnoreCase(q, fields)` - JPA `Specification`으로 변환해 `JpaSpecificationExecutor.findAll(Specification, Sort)`에 전달한다.
- 정렬: `com.naroom.api.admin.common.AdminSortParser.parse(sort, allowedFields, fallback)` - allowlist 밖 필드는 즉시 거부해 임의 프로퍼티 접근(HQL 예외, 의도치 않은 컬럼 노출)을 막는다.
- 새 관리자 목록 API를 추가할 때는 이 두 유틸리티와 이 표의 패턴을 그대로 따른다.

## 4. 프론트 적용

`naroom-admin`의 목록 화면은 검색창(입력 시 디바운스)과 정렬 가능한 컬럼 헤더 클릭으로 이 파라미터를 채운다. 필터·검색 상태는 Admin Web Implementation Spec §6.1에 따라 URL query string에 보존하는 것을 권장하되, 이번 v1 구현 범위는 아니다.
