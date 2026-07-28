-- Naroom Beta 1
-- LifeTime 도메인 1단계(데이터 구조). docs/domain/lifetime-calendar.md,
-- docs/api/ai-policy-architecture.md 24.4(4-E)/24.5(5-F) 기준으로 기간별 회고·나의 정리 테이블을 생성한다.
-- Calendar는 별도 테이블 없이 entries/check_ins/period_reflections를 날짜 기준으로 조회하는 기능이다.
--
-- 3일/주간 회고는 같은 형식·기간만 다르므로(§3.2, 2026-07-27 정정) 테이블을 하나로 만들고
-- feature_type(THREE_DAY_REFLECTION/WEEKLY_REFLECTION) + period_start/period_end로 구분한다
-- (docs/database/reference/naroom_beta1_full_schema_reference.sql의 weekly_reflections는 주간 전용으로
-- 설계되어 있어 그대로 쓰지 않는다. docs/domain/lifetime-calendar.md "스키마 충돌과 결정" 참고).
--
-- period_reflections는 ai_reflections와 같은 패턴으로 설계한다: 실제 회고 텍스트는 이 테이블에 저장하고,
-- 모델·토큰·프롬프트 버전·안전 분류 이력은 기존 ai_generation_runs에 맡긴다. status는 새 enum을 만들지 않고
-- 기존 ai_job_status를 재사용해 개별 기록 회고(ai_reflections)와 동일한 상태 전이 규칙을 따른다.

ALTER TYPE "entry_type" ADD VALUE 'THREE_DAY_REFLECTION';

CREATE TYPE "summary_scope" AS ENUM (
    'CURRENT_SELF',
    'WEEKLY',
    'MONTHLY',
    'QUARTERLY',
    'EXPERIMENT'
);

-- ---------------------------------------------------------------------------
-- 기간별 회고 (3일/주간)
-- ---------------------------------------------------------------------------

CREATE TABLE "period_reflections" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
    "entry_id" uuid NOT NULL UNIQUE REFERENCES "entries" ("id") ON DELETE CASCADE,
    "feature_type" ai_feature_type NOT NULL,
    "period_start" date NOT NULL,
    "period_end" date NOT NULL,
    "version_no" integer NOT NULL DEFAULT 1,
    "previous_reflection_id" uuid REFERENCES "period_reflections" ("id"),
    "status" ai_job_status NOT NULL DEFAULT 'PENDING',
    "generation_run_id" uuid REFERENCES "ai_generation_runs" ("id"),
    "summary_text" text,
    "insights" jsonb,
    "question_text" text,
    "safety_code" varchar(50),
    "error_code" varchar(80),
    "requested_at" timestamptz NOT NULL DEFAULT now(),
    "completed_at" timestamptz,
    CONSTRAINT "ck_period_reflections_feature_type" CHECK (feature_type IN ('THREE_DAY_REFLECTION', 'WEEKLY_REFLECTION')),
    CONSTRAINT "ck_period_reflections_period" CHECK (period_end >= period_start),
    CONSTRAINT "uq_period_reflections_member_period_version" UNIQUE ("member_id", "feature_type", "period_start", "version_no")
);
COMMENT ON TABLE "period_reflections" IS '3일/주간 회고(같은 형식, 기간만 다름); entry_id는 LifeTime에 표시할 기록 봉투';
COMMENT ON COLUMN "period_reflections"."id" IS '기간별 회고 ID';
COMMENT ON COLUMN "period_reflections"."member_id" IS '회고 회원';
COMMENT ON COLUMN "period_reflections"."entry_id" IS 'LifeTime 타임라인에 표시할 기록 봉투(entry_type=THREE_DAY_REFLECTION 또는 WEEKLY_REFLECTION)';
COMMENT ON COLUMN "period_reflections"."feature_type" IS '3일 회고/주간 회고 구분(ai_feature_type 재사용, CHECK로 다른 값 배제)';
COMMENT ON COLUMN "period_reflections"."period_start" IS '회고 대상 기간 시작일(회원 시간대 기준)';
COMMENT ON COLUMN "period_reflections"."period_end" IS '회고 대상 기간 종료일(포함)';
COMMENT ON COLUMN "period_reflections"."version_no" IS '같은 기간을 재생성할 때 증가하는 버전';
COMMENT ON COLUMN "period_reflections"."previous_reflection_id" IS '재생성 시 이전 버전 참조';
COMMENT ON COLUMN "period_reflections"."status" IS 'AI 생성 상태(ai_reflections와 동일한 ai_job_status 재사용)';
COMMENT ON COLUMN "period_reflections"."generation_run_id" IS '실제 모델 호출 이력(토큰·안전 분류는 ai_generation_runs가 전담)';
COMMENT ON COLUMN "period_reflections"."summary_text" IS '기록 근거 기반 기간 요약';
COMMENT ON COLUMN "period_reflections"."insights" IS '감정·상황·감사·시도·도움 조건의 구조화 결과';
COMMENT ON COLUMN "period_reflections"."question_text" IS '이 기간을 살펴볼 질문';
COMMENT ON COLUMN "period_reflections"."safety_code" IS '출력이 RESTRICTED/CRISIS로 판정된 경우의 사유 코드';
COMMENT ON COLUMN "period_reflections"."error_code" IS '생성 실패 시 내부 에러 코드(공개 노출 안 함)';
COMMENT ON COLUMN "period_reflections"."requested_at" IS '회고 요청(작업 생성) 시각';
COMMENT ON COLUMN "period_reflections"."completed_at" IS '생성 완료·차단·실패 확정 시각';
CREATE INDEX "ix_period_reflections_member_period" ON "period_reflections" ("member_id", "feature_type", "period_start");

CREATE TABLE "period_reflection_entries" (
    "period_reflection_id" uuid NOT NULL REFERENCES "period_reflections" ("id") ON DELETE CASCADE,
    "entry_id" uuid NOT NULL REFERENCES "entries" ("id") ON DELETE CASCADE,
    "evidence_role" varchar(40),
    "linked_at" timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY ("period_reflection_id", "entry_id")
);
COMMENT ON TABLE "period_reflection_entries" IS '기간별 회고의 근거가 된 기록 연결';
COMMENT ON COLUMN "period_reflection_entries"."period_reflection_id" IS '기간별 회고 ID';
COMMENT ON COLUMN "period_reflection_entries"."entry_id" IS '근거 기록 ID';
COMMENT ON COLUMN "period_reflection_entries"."evidence_role" IS 'EMOTION/GRATITUDE/EFFORT/RECOVERY 등 근거 역할';
COMMENT ON COLUMN "period_reflection_entries"."linked_at" IS '연결 시각';
CREATE INDEX "ix_period_reflection_entries_entry" ON "period_reflection_entries" ("entry_id");

-- ---------------------------------------------------------------------------
-- 나의 정리
-- ---------------------------------------------------------------------------

CREATE TABLE "personal_summaries" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
    "entry_id" uuid NOT NULL UNIQUE REFERENCES "entries" ("id") ON DELETE CASCADE,
    "scope" summary_scope NOT NULL DEFAULT 'CURRENT_SELF',
    "period_start" date,
    "period_end" date,
    "archived_at" timestamptz,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT "ck_personal_summaries_period_pair" CHECK (period_end IS NULL OR period_start IS NOT NULL),
    CONSTRAINT "ck_personal_summaries_period_order" CHECK (period_end IS NULL OR period_end >= period_start)
);
COMMENT ON TABLE "personal_summaries" IS 'AI 분석과 구분되는 사용자 작성 나의 정리; 실제 글 내용은 entries.body(SELF_SUMMARY)에 있고 이 테이블은 범위·기간·보관 메타데이터만 담당';
COMMENT ON COLUMN "personal_summaries"."id" IS '자기정리 ID';
COMMENT ON COLUMN "personal_summaries"."member_id" IS '작성 회원';
COMMENT ON COLUMN "personal_summaries"."entry_id" IS 'SELF_SUMMARY 기록 봉투(실제 글 내용 위치)';
COMMENT ON COLUMN "personal_summaries"."scope" IS '현재/주간/월간/분기/실험 범위';
COMMENT ON COLUMN "personal_summaries"."period_start" IS '정리 대상 기간 시작일; CURRENT_SELF는 null';
COMMENT ON COLUMN "personal_summaries"."period_end" IS '정리 대상 기간 종료일; CURRENT_SELF는 null';
COMMENT ON COLUMN "personal_summaries"."archived_at" IS '이후 새 정리로 대체되어 과거 관점으로 보관된 시각';
COMMENT ON COLUMN "personal_summaries"."created_at" IS '생성 시각';
COMMENT ON COLUMN "personal_summaries"."updated_at" IS '수정 시각';
CREATE INDEX "ix_personal_summaries_member_recent" ON "personal_summaries" ("member_id", "created_at");
CREATE TRIGGER "trg_personal_summaries_updated_at" BEFORE UPDATE ON "personal_summaries" FOR EACH ROW EXECUTE FUNCTION set_updated_at();
