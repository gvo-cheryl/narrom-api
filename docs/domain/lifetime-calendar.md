# LifeTime & Calendar 도메인

상태: Draft (1단계 데이터 구조)
관련 이슈: #21(BE - Lifetime & Calendar), #26/#27(AI 도메인 — 기간별 회고는 이 도메인의 데이터 모델에 의존)

이 문서는 아직 확정되지 않은 API·화면 상세를 임의로 채우지 않습니다. 아래는 현재까지 확정된 사실과 그 근거만 기록합니다.

## 범위

LifeTime은 타임라인·캘린더·감정/에너지 흐름·태그 탐색·기록 상세·기간별 회고(3일/주간)·나의 정리로 구성된 자기이해 도메인입니다(`docs/foundation` IA/PRD 문서 기준). 이번 1단계는 이 중 **데이터 구조**만 확정합니다.

- **Calendar는 독립 도메인·테이블이 아닙니다.** `entries`/`check_ins`/`period_reflections`/`personal_summaries`를 날짜 기준으로 조회하는 기능이며, 신규 테이블을 만들지 않습니다(`docs/instruction/0724_Domain-Implementation-Plan.md` §7).
- **월간·분기·연간 회고는 Beta 1 필수 화면에서 제외**하되, `personal_summaries.scope`가 이미 `MONTHLY`/`QUARTERLY`를 포함해 저장 구조는 확장 가능하게 유지합니다(`docs/api/ai-policy-architecture.md` §3.2, 2026-07-27).
- **외부 캘린더 연동, 회고 후속 AI 대화는 이번 범위에서 제외**합니다(같은 절).
- 작은 실험 피드백/회고는 기간별 회고와 별개 기능입니다. 실험 리뷰가 일반 `Entry`(`EXPERIMENT_REVIEW`)로 기록되면 기존 `ENTRY_REFLECTION` 파이프라인을 그대로 재사용할 가능성이 높고, 이 도메인의 신규 테이블을 필요로 하지 않습니다(같은 절).

## 스키마 충돌과 결정 (2026-07-28)

`docs/database/reference/naroom_beta1_full_schema_reference.sql`에는 `weekly_reflections`가 **주간 전용**으로 이미 설계되어 있습니다(`CHECK(week_end = week_start + 6)`, 3일 회고 테이블 없음). 반면 `docs/api/ai-policy-architecture.md` §24.4(4-E, 2026-07-27)는 "3일/주간 회고는 같은 형식·기간만 다르므로 테이블을 하나로 만들고 `feature_type` + 기간 시작·종료일로 구분해야 한다 — 별도 테이블 두 개로 쪼개지 않는다"고 명시합니다.

**결정: `ai-policy-architecture.md`를 따른다.** 레퍼런스 SQL의 `weekly_reflections`/`weekly_reflection_entries`는 그대로 쓰지 않고, 아래처럼 `period_reflections`/`period_reflection_entries`로 통합·재설계합니다.

## 확정된 데이터 설계 (V13, 이번 마이그레이션)

### `entry_type`에 `THREE_DAY_REFLECTION` 추가

기존 `entry_type` enum에는 `WEEKLY_REFLECTION`만 있고 `THREE_DAY_REFLECTION`이 없습니다(`V2__create_content_record_checkin.sql`). 기간별 회고도 다른 구조화 기록과 같은 방식으로 `entries`에 봉투 행을 만들어 LifeTime 타임라인에 노출하므로, enum에 값을 추가합니다(`ALTER TYPE ... ADD VALUE`, 하위 호환 — 기존 값·행에 영향 없음).

### `period_reflections`

3일/주간 회고를 하나의 테이블로 통합합니다. `ai_reflections`(개별 기록 회고)와 최대한 같은 패턴을 씁니다 — 실제 회고 텍스트는 이 테이블에 저장하고, 모델·토큰·프롬프트 버전·안전 분류 이력은 기존 `ai_generation_runs`에 맡깁니다. `status`는 새 enum을 만들지 않고 기존 `ai_job_status`를 재사용해 개별 기록 회고와 동일한 상태 전이 규칙(PENDING→PROCESSING→COMPLETED/BLOCKED/SAFETY_SUPPORT/FAILED)을 따릅니다.

| 컬럼 | 설명 |
|---|---|
| `entry_id` | LifeTime 타임라인에 표시할 기록 봉투(`entry_type`=`THREE_DAY_REFLECTION` 또는 `WEEKLY_REFLECTION`), `UNIQUE` |
| `feature_type` | `ai_feature_type` 재사용, `THREE_DAY_REFLECTION`/`WEEKLY_REFLECTION`만 허용(CHECK로 다른 값 배제) |
| `period_start`/`period_end` | 회고 대상 기간(회원 시간대 기준, `period_end` 포함) |
| `version_no` | 같은 기간을 재생성할 때 증가(§6.2 "부정 평가 후 재생성" 및 §12.3 "매번 새로 생성" 원칙) |
| `previous_reflection_id` | 재생성 시 이전 버전 참조(자기참조, nullable) |
| `status`, `generation_run_id`, `summary_text`, `insights`, `question_text`, `safety_code`, `error_code` | `ai_reflections`와 동일한 역할 |

`UNIQUE(member_id, feature_type, period_start, version_no)`로 같은 기간·버전의 중복 생성을 막습니다(0725 스키마 리뷰가 `weekly_reflections`에 권고했던 `version_no`/`previous_reflection_id`/복합 UNIQUE를 그대로 반영).

### `period_reflection_entries`

기간별 회고의 근거가 된 기록 연결(`weekly_reflection_entries`와 동일한 구조, 테이블명만 통일).

### `personal_summaries`

"나의 정리"의 범위·기간·보관 메타데이터만 담당합니다. **실제 글 내용은 이 테이블이 아니라 `entries.body`(`entry_type`=`SELF_SUMMARY`)에 있습니다** — 레퍼런스 SQL에도 별도 본문 컬럼이 없어 같은 설계를 따릅니다. `scope`는 `summary_scope`(`CURRENT_SELF`/`WEEKLY`/`MONTHLY`/`QUARTERLY`/`EXPERIMENT`) 신규 enum으로, 월간·분기 값은 화면은 없어도 데이터 구조는 확장 가능하게 유지하기 위해 포함합니다.

`entry_self_reflections`(개별 기록에 대한 사용자 후기, `EntrySelfReflection`)와는 다른 개념입니다 — 그건 기록 1건에 종속된 짧은 의견이고, `personal_summaries`는 기간·범위를 갖는 독립된 자기정리입니다.

## 2단계: 캘린더·타임라인 조회 (2026-07-28)

- `GET /api/v1/lifetime/timeline`(from/to/entryType 선택) — 태그(`CONFIRMED`/`SYSTEM` 상태만, `SUGGESTED`는 제외)·AI 상태(`AiJob` 최신 것)·자기정리 존재 여부까지 담은 전용 응답(`EntryTimelineResponse`). 기존 `GET /record/entries`(`EntryResponse`)는 create/update/publish 등에서도 쓰는 가벼운 응답이라 그대로 두고 건드리지 않음 — 이 집계를 얹으면 단순 CUD 응답까지 매번 태그·AI작업·자기회고를 조회하게 되기 때문(coverage-checklist §공백 8 결정).
- `GET /api/v1/lifetime/calendar?year=&month=` — 날짜별 기록/체크인 존재 여부만 반환(존재 여부만, 감정 등 대표값 집계는 5단계로 남김). 기록하지 않은 날도 그대로 포함해 "기록 없음"과 "0점"을 구분한다.
- Calendar/Timeline은 새 도메인 패키지 없이 `com.naroom.api.lifetime`에 두되, 엔드포인트는 `/api/v1/lifetime/*`로 통일(coverage-checklist가 제안했던 `/record/calendar` 등은 잠정안이라 그대로 쓰지 않음 — LifeTime이라는 상위 도메인 이름과 일치시킴).
- 배치 조회 성능을 위해 `EntryTagRepository`/`AiJobRepository`/`EntrySelfReflectionRepository`에 `_IdIn` 계열 메서드 추가(엔트리 목록당 N+1 방지).

## 3단계: 기간별 회고 파이프라인 (진행 중, 이슈 #30)

**최소 기록 수 결정(2026-07-28):** PRD §13이 "주간 회고를 제공하기 위한 최소 기록 수는 몇 개인가?"를 열린 질문으로 남겨뒀던 것에 대한 답 — 주간 회고 3건, 3일 회고 1건. 3일 회고는 근거가 1건뿐이어도 진행하되, 응답 품질이 제한적일 수 있다는 것은 AI 지침 차원에서 스스로 밝히게 한다(3-B에서 반영).

**3일 회고 기간 경계(2026-07-28):** 달력 고정 블록이 아니라 요청 시점 기준 롤링 윈도우(오늘 포함 최근 3일) — 매일 요청 가능. 주간 회고는 가장 최근에 완전히 끝난 ISO 주(월~일)로 계산해 "주차당 1개"를 자연스럽게 만족시킨다.

- [x] 3-A. 기간 계산(`PeriodCalculator`) + 자격 확인·근거 기록 선별(`PeriodReflectionEligibilityService`, `LifetimeErrorCode`). §12.4의 정교한 선별 기준(변화량 큰 기록, 편향 방지 무작위 표본 등)은 이번 1차 구현에서는 적용하지 않고 "기간 내 발행된 기록 전부"로 단순화 — 다음 반복 과제로 남김. 회고 봉투 자체(`WEEKLY_REFLECTION`/`THREE_DAY_REFLECTION`)와 `SELF_SUMMARY`는 근거에서 제외
- [ ] 3-B. 기간별 프롬프트 조립 + 출력 스키마·파서
- [ ] 3-C. 파이프라인 연결(생성 요청, 처리기, 결과 저장) + 근거 기록 연결
- [ ] 3-D. 조회 API

## 아직 구현되지 않은 것

- 나의 정리 CRUD API
- 감정·에너지 흐름, 태그 탐색 집계 API
