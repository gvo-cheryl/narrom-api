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

## 아직 구현되지 않은 것

- Calendar 조회 API(월별 존재 여부, 날짜별 상세)
- 타임라인 통합 조회 API(태그·감정·AI 상태·자기정리 집계 — 현재 `GET /record/entries`는 원문만 반환)
- 날짜 범위로 `Entry`/`CheckIn`을 조회하는 리포지토리 메서드
- 기간별 회고 생성·조회 API 및 AI 파이프라인 연동(§24.5 5-F)
- 나의 정리 CRUD API
- 감정·에너지 흐름, 태그 탐색 집계 API
